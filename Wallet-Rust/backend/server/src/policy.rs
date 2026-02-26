use dashmap::DashMap;
use std::sync::Arc;
use tokio::sync::Semaphore;

pub(crate) const ALLOWED_RPC_METHODS: &[&str] = &[
    "eth_chainId",
    "net_version",
    "web3_clientVersion",
    "eth_blockNumber",
    "eth_getBalance",
    "eth_getCode",
    "eth_getTransactionCount",
    "eth_getBlockByNumber",
    "eth_getBlockByHash",
    "eth_getTransactionByHash",
    "eth_getTransactionReceipt",
    "eth_getLogs",
    "eth_call",
    "eth_estimateGas",
    "eth_gasPrice",
    "eth_maxPriorityFeePerGas",
    "eth_feeHistory",
    "eth_sendRawTransaction",
];

pub(crate) fn is_allowed_rpc_method(method: &str) -> bool {
    ALLOWED_RPC_METHODS.contains(&method)
}

pub(crate) fn enforce_rpc_response_size_limit(size: usize, max: usize) -> Result<(), String> {
    if size > max {
        return Err(format!(
            "RPC response too large ({} bytes > {} bytes)",
            size, max
        ));
    }
    Ok(())
}

pub(crate) fn host_semaphore(
    semaphores: &DashMap<String, Arc<Semaphore>>,
    host: &str,
    limit: usize,
) -> Arc<Semaphore> {
    semaphores
        .entry(host.to_string())
        .or_insert_with(|| Arc::new(Semaphore::new(limit.max(1))))
        .clone()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn allowed_rpc_methods_reject_unknown() {
        assert!(is_allowed_rpc_method("eth_getBalance"));
        assert!(!is_allowed_rpc_method("personal_sign"));
    }

    #[test]
    fn rpc_response_size_limit_enforced() {
        assert!(enforce_rpc_response_size_limit(1024, 2048).is_ok());
        assert!(enforce_rpc_response_size_limit(4096, 2048).is_err());
    }

    #[tokio::test]
    async fn per_host_semaphore_caps_parallelism() {
        let map: DashMap<String, Arc<Semaphore>> = DashMap::new();
        let sem = host_semaphore(&map, "rpc.ankr.com", 1);
        let _p1 = sem.acquire().await.expect("first permit");
        assert!(sem.try_acquire().is_err());

        let same = host_semaphore(&map, "rpc.ankr.com", 1);
        assert!(Arc::ptr_eq(&sem, &same));
    }
}
