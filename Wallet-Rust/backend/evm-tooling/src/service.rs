use std::{
    collections::HashMap,
    path::{Path, PathBuf},
    sync::Arc,
    time::{Duration, SystemTime, UNIX_EPOCH},
};

use anyhow::{anyhow, Context, Result};
use ethers::{
    abi::Abi,
    contract::{Contract, ContractFactory},
    core::types::{Address, U256},
    middleware::SignerMiddleware,
    providers::{Http, Middleware, Provider},
    signers::{LocalWallet, Signer},
    types::Bytes,
};

use crate::{
    artifacts::{artifacts_dir_default, load_evm_artifact},
    types::{DeployParams, DeployedTestnetAmmAddresses, LiquidityParams, MintParams, TokenMeta},
};

type Client = Arc<SignerMiddleware<Provider<Http>, LocalWallet>>;

fn parse_amount_ether(raw: Option<&str>, fallback: &str) -> Result<U256> {
    let s = raw.unwrap_or(fallback);
    ethers::utils::parse_ether(s).map_err(|e| anyhow!("invalid ether amount {s}: {e}"))
}

fn token_name_map(symbol: &str) -> String {
    match symbol {
        "WETH" => "Wrapped Ether",
        "WBNB" => "Wrapped BNB",
        "WMATIC" => "Wrapped Matic",
        "WAVAX" => "Wrapped AVAX",
        "USDC" => "USD Coin",
        "USDT" => "Tether USD",
        "DAI" => "Dai Stablecoin",
        "WBTC" => "Wrapped Bitcoin",
        s => return format!("Test {s}"),
    }
    .to_string()
}

fn parse_token_list(base_symbol: &str, raw: Option<&str>) -> Vec<String> {
    let mut symbols: Vec<String> = raw
        .unwrap_or("")
        .split(',')
        .map(|s| s.trim().to_uppercase())
        .filter(|s| !s.is_empty())
        .collect();
    if symbols.is_empty() {
        symbols = vec![
            base_symbol.to_string(),
            "USDC".to_string(),
            "USDT".to_string(),
            "DAI".to_string(),
            "WBTC".to_string(),
        ];
    }
    if !symbols.iter().any(|s| s == base_symbol) {
        symbols.insert(0, base_symbol.to_string());
    }
    symbols
}

fn deployed_file(repo_root: &Path, chain: &str) -> PathBuf {
    repo_root
        .join("Wallet-Rust")
        .join("backend")
        .join("state")
        .join(format!("deployed_testnet_amm_{chain}.json"))
}

async fn build_client(rpc_url: &str, private_key: &str) -> Result<Client> {
    let provider = Provider::<Http>::try_from(rpc_url)
        .with_context(|| format!("invalid rpc url {rpc_url}"))?
        .interval(Duration::from_millis(400));
    let chain_id = provider.get_chainid().await?.as_u64();
    let wallet = private_key.parse::<LocalWallet>()?.with_chain_id(chain_id);
    Ok(Arc::new(SignerMiddleware::new(provider, wallet)))
}

fn parse_abi(value: serde_json::Value) -> Result<Abi> {
    // Accept both canonical Hardhat "abi: [ ... ]" and wrapped "abi: { value: [ ... ] }".
    let normalized = match value {
        serde_json::Value::Object(mut map) => match map.remove("value") {
            Some(v @ serde_json::Value::Array(_)) => v,
            _ => serde_json::Value::Object(map),
        },
        other => other,
    };
    serde_json::from_value(normalized).map_err(|e| anyhow!("parse abi: {e}"))
}

fn parse_bytecode(hex: &str) -> Result<Bytes> {
    hex.parse::<Bytes>()
        .map_err(|e| anyhow!("parse bytecode: {e}"))
}

async fn wait_tx<T, E>(pending: T) -> Result<()>
where
    T: std::future::Future<
        Output = std::result::Result<Option<ethers::types::TransactionReceipt>, E>,
    >,
    E: std::error::Error + Send + Sync + 'static,
{
    let _ = pending.await.map_err(|e| anyhow!(e))?;
    Ok(())
}

pub async fn deploy_testnet_amm(
    repo_root: &Path,
    private_key: &str,
    p: DeployParams,
) -> Result<DeployedTestnetAmmAddresses> {
    let artifacts_dir = artifacts_dir_default(repo_root);
    let client = build_client(&p.rpc_url, private_key).await?;
    let deployer_address = client.address();

    let bal = client.get_balance(deployer_address, None).await?;
    if bal.is_zero() {
        return Err(anyhow!("Deployer has 0 balance on selected chain"));
    }

    let art_factory = load_evm_artifact(
        &artifacts_dir,
        "contracts/amm/UniswapV2Factory.sol/UniswapV2Factory.json",
    )?;
    let art_router = load_evm_artifact(
        &artifacts_dir,
        "contracts/amm/UniswapV2Router.sol/UniswapV2Router.json",
    )?;
    let art_mock = load_evm_artifact(&artifacts_dir, "contracts/amm/MockERC20.sol/MockERC20.json")?;
    let art_wrapped = load_evm_artifact(
        &artifacts_dir,
        "contracts/amm/WrappedNative.sol/WrappedNative.json",
    )?;

    let factory_cf = ContractFactory::new(
        parse_abi(art_factory.abi)?,
        parse_bytecode(&art_factory.bytecode)?,
        client.clone(),
    );
    let factory = factory_cf.deploy(())?.send().await?;
    let factory_addr = factory.address();

    let router_cf = ContractFactory::new(
        parse_abi(art_router.abi.clone())?,
        parse_bytecode(&art_router.bytecode)?,
        client.clone(),
    );
    let router = router_cf.deploy(factory_addr)?.send().await?;
    let router_addr = router.address();

    let base_symbol = match p.chain.as_str() {
        "sepolia" => "WETH",
        "bsc" => "WBNB",
        "amoy" => "WMATIC",
        "fuji" => "WAVAX",
        _ => "WETH",
    }
    .to_string();

    let token_symbols = parse_token_list(&base_symbol, p.tokens.as_deref());
    let mint_amount = parse_amount_ether(p.mint_amount.as_deref(), "1000000")?;
    let approve_amount = parse_amount_ether(p.approve_amount.as_deref(), "500000")?;
    let liquidity_amount = parse_amount_ether(p.liquidity_amount.as_deref(), "100000")?;
    let liquidity_min = parse_amount_ether(p.liquidity_min.as_deref(), "99000")?;

    let mock_abi = parse_abi(art_mock.abi.clone())?;
    let wrapped_abi = parse_abi(art_wrapped.abi.clone())?;
    let token_cf = ContractFactory::new(
        mock_abi.clone(),
        parse_bytecode(&art_mock.bytecode)?,
        client.clone(),
    );
    let wrapped_cf = ContractFactory::new(
        wrapped_abi.clone(),
        parse_bytecode(&art_wrapped.bytecode)?,
        client.clone(),
    );

    let mut token_contracts: HashMap<
        String,
        Contract<SignerMiddleware<Provider<Http>, LocalWallet>>,
    > = HashMap::new();
    let mut tokens: HashMap<String, String> = HashMap::new();
    let mut tokens_meta: HashMap<String, TokenMeta> = HashMap::new();

    for sym in &token_symbols {
        let name = token_name_map(sym);
        let contract = if sym == &base_symbol {
            wrapped_cf
                .clone()
                .deploy((name.clone(), sym.clone()))?
                .send()
                .await?
        } else {
            token_cf
                .clone()
                .deploy((name.clone(), sym.clone()))?
                .send()
                .await?
        };
        let c = Contract::new(
            contract.address(),
            if sym == &base_symbol {
                wrapped_abi.clone()
            } else {
                mock_abi.clone()
            },
            client.clone(),
        );
        token_contracts.insert(sym.clone(), c);
        tokens.insert(sym.clone(), format!("{:#x}", contract.address()));
    }

    for sym in &token_symbols {
        let c = token_contracts
            .get(sym)
            .ok_or_else(|| anyhow!("missing token contract"))?;
        if sym == &base_symbol {
            wait_tx(
                c.method::<_, ()>("deposit", ())?
                    .value(mint_amount)
                    .send()
                    .await?,
            )
            .await?;
        } else {
            wait_tx(
                c.method::<_, ()>("mint", (deployer_address, mint_amount))?
                    .send()
                    .await?,
            )
            .await?;
        }
    }

    for sym in &token_symbols {
        let c = token_contracts
            .get(sym)
            .ok_or_else(|| anyhow!("missing token contract"))?;
        wait_tx(
            c.method::<_, ()>("approve", (router_addr, approve_amount))?
                .send()
                .await?,
        )
        .await?;
    }

    let base_addr: Address = token_contracts
        .get(&base_symbol)
        .ok_or_else(|| anyhow!("missing base token"))?
        .address();
    let router_c = Contract::new(
        router_addr,
        parse_abi(art_router.abi.clone())?,
        client.clone(),
    );

    let deadline = U256::from(
        SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_secs()
            + (60 * 20),
    );

    for sym in &token_symbols {
        if sym == &base_symbol {
            continue;
        }
        let token_addr = token_contracts
            .get(sym)
            .ok_or_else(|| anyhow!("missing token contract"))?
            .address();
        wait_tx(
            router_c
                .method::<_, ()>(
                    "addLiquidity",
                    (
                        base_addr,
                        token_addr,
                        liquidity_amount,
                        liquidity_amount,
                        liquidity_min,
                        liquidity_min,
                        deployer_address,
                        deadline,
                    ),
                )?
                .send()
                .await?,
        )
        .await?;
    }

    if let Some(target) = p.target_address.as_deref() {
        let target_addr: Address = target.parse().context("invalid targetAddress")?;
        let ten_k = parse_amount_ether(Some("10000"), "10000")?;
        for sym in &token_symbols {
            let c = token_contracts
                .get(sym)
                .ok_or_else(|| anyhow!("missing token contract"))?;
            if sym == &base_symbol {
                wait_tx(
                    c.method::<_, ()>("transfer", (target_addr, ten_k))?
                        .send()
                        .await?,
                )
                .await?;
            } else {
                wait_tx(
                    c.method::<_, ()>("mint", (target_addr, ten_k))?
                        .send()
                        .await?,
                )
                .await?;
            }
        }
    }

    for (sym, c) in token_contracts {
        let decimals = c
            .method::<_, u8>("decimals", ())?
            .call()
            .await
            .unwrap_or(18);
        tokens_meta.insert(
            sym.clone(),
            TokenMeta {
                address: format!("{:#x}", c.address()),
                symbol: sym,
                decimals,
            },
        );
    }

    let out = DeployedTestnetAmmAddresses {
        uniswap_v2_factory: format!("{:#x}", factory_addr),
        uniswap_v2_router: format!("{:#x}", router_addr),
        tokens,
        tokens_meta,
        base_token: base_symbol,
        network: p.chain.clone(),
    };

    let out_file = deployed_file(repo_root, &p.chain);
    if let Some(dir) = out_file.parent() {
        std::fs::create_dir_all(dir).ok();
    }
    std::fs::write(&out_file, serde_json::to_string_pretty(&out)?)?;
    Ok(out)
}

pub fn load_deployed(repo_root: &Path, chain: &str) -> Result<DeployedTestnetAmmAddresses> {
    let p = deployed_file(repo_root, chain);
    let raw = std::fs::read_to_string(&p).with_context(|| format!("read {}", p.display()))?;
    let v: DeployedTestnetAmmAddresses = serde_json::from_str(&raw)?;
    Ok(v)
}

pub async fn mint_testnet_token(repo_root: &Path, private_key: &str, p: MintParams) -> Result<()> {
    let deployed = load_deployed(repo_root, &p.chain)?;
    let artifacts_dir = artifacts_dir_default(repo_root);
    let client = build_client(&p.rpc_url, private_key).await?;
    let token_symbol = p.token_symbol.to_uppercase();
    let target: Address = p.target_address.parse().context("invalid targetAddress")?;
    let amount = parse_amount_ether(Some(&p.amount), "0")?;

    let base_symbol = deployed.base_token.to_uppercase();
    let token_addr: Address = deployed
        .tokens
        .get(&token_symbol)
        .ok_or_else(|| anyhow!("Unknown token symbol"))?
        .parse()?;
    let art_mock = load_evm_artifact(&artifacts_dir, "contracts/amm/MockERC20.sol/MockERC20.json")?;
    let art_wrapped = load_evm_artifact(
        &artifacts_dir,
        "contracts/amm/WrappedNative.sol/WrappedNative.json",
    )?;

    if token_symbol == base_symbol {
        let c = Contract::new(token_addr, parse_abi(art_wrapped.abi)?, client.clone());
        let wrapped_flow = async {
            wait_tx(
                c.method::<_, ()>("deposit", ())?
                    .value(amount)
                    .send()
                    .await?,
            )
            .await?;
            wait_tx(
                c.method::<_, ()>("transfer", (target, amount))?
                    .send()
                    .await?,
            )
            .await?;
            Ok::<(), anyhow::Error>(())
        }
        .await;

        if wrapped_flow.is_err() {
            // Some legacy testnet deploys use MockERC20 for the configured base symbol.
            let mock = Contract::new(token_addr, parse_abi(art_mock.abi)?, client.clone());
            wait_tx(mock.method::<_, ()>("mint", (target, amount))?.send().await?).await?;
        }
    } else {
        let c = Contract::new(token_addr, parse_abi(art_mock.abi)?, client.clone());
        wait_tx(c.method::<_, ()>("mint", (target, amount))?.send().await?).await?;
    }
    Ok(())
}

pub async fn add_liquidity(repo_root: &Path, private_key: &str, p: LiquidityParams) -> Result<()> {
    let deployed = load_deployed(repo_root, &p.chain)?;
    let artifacts_dir = artifacts_dir_default(repo_root);
    let client = build_client(&p.rpc_url, private_key).await?;
    let signer = client.address();
    let recipient: Address = match p.target_address.as_deref() {
        Some(v) if !v.trim().is_empty() => v.parse()?,
        _ => signer,
    };

    let token_symbol = p.token_symbol.to_uppercase();
    let base_symbol = deployed.base_token.to_uppercase();
    if token_symbol == base_symbol {
        return Err(anyhow!(
            "Liquidity tokenSymbol must be different from base token"
        ));
    }

    let base_addr: Address = deployed
        .tokens
        .get(&base_symbol)
        .ok_or_else(|| anyhow!("Missing base token"))?
        .parse()?;
    let token_addr: Address = deployed
        .tokens
        .get(&token_symbol)
        .ok_or_else(|| anyhow!("Unknown token symbol"))?
        .parse()?;
    let router_addr: Address = deployed.uniswap_v2_router.parse()?;

    let base_amount = parse_amount_ether(Some(&p.base_amount), "0")?;
    let token_amount = parse_amount_ether(Some(&p.token_amount), "0")?;
    let min_base = parse_amount_ether(p.min_base_amount.as_deref(), &p.base_amount)?;
    let min_token = parse_amount_ether(p.min_token_amount.as_deref(), &p.token_amount)?;

    let art_mock = load_evm_artifact(&artifacts_dir, "contracts/amm/MockERC20.sol/MockERC20.json")?;
    let art_wrapped = load_evm_artifact(
        &artifacts_dir,
        "contracts/amm/WrappedNative.sol/WrappedNative.json",
    )?;
    let art_router = load_evm_artifact(
        &artifacts_dir,
        "contracts/amm/UniswapV2Router.sol/UniswapV2Router.json",
    )?;
    let base_c = Contract::new(base_addr, parse_abi(art_wrapped.abi)?, client.clone());
    let token_c = Contract::new(token_addr, parse_abi(art_mock.abi)?, client.clone());
    let router_c = Contract::new(router_addr, parse_abi(art_router.abi)?, client.clone());

    let base_balance: U256 = base_c
        .method::<_, U256>("balanceOf", signer)?
        .call()
        .await?;
    if base_balance < base_amount {
        wait_tx(
            base_c
                .method::<_, ()>("deposit", ())?
                .value(base_amount - base_balance)
                .send()
                .await?,
        )
        .await?;
    }
    let token_balance: U256 = token_c
        .method::<_, U256>("balanceOf", signer)?
        .call()
        .await?;
    if token_balance < token_amount {
        wait_tx(
            token_c
                .method::<_, ()>("mint", (signer, token_amount - token_balance))?
                .send()
                .await?,
        )
        .await?;
    }

    wait_tx(
        base_c
            .method::<_, ()>("approve", (router_addr, base_amount))?
            .send()
            .await?,
    )
    .await?;
    wait_tx(
        token_c
            .method::<_, ()>("approve", (router_addr, token_amount))?
            .send()
            .await?,
    )
    .await?;

    let deadline = U256::from(
        SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_secs()
            + 60 * 20,
    );

    wait_tx(
        router_c
            .method::<_, ()>(
                "addLiquidity",
                (
                    base_addr,
                    token_addr,
                    base_amount,
                    token_amount,
                    min_base,
                    min_token,
                    recipient,
                    deadline,
                ),
            )?
            .send()
            .await?,
    )
    .await?;

    Ok(())
}
