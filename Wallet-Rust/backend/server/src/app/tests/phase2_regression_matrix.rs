use super::*;
use axum::http::Method;
use serde_json::json;

#[tokio::test]
async fn phase2_financial_routes_reject_unknown_query_fields_with_stable_4xx_shape() {
    let _l = global_test_lock().await;
    let app = build_app(test_config()).await.expect("build app");

    let cases = vec![
        (
            Method::GET,
            "/api/proxy/history?url=https%3A%2F%2Fapi.coingecko.com%2Fapi%2Fv3%2Fping&unexpected=1",
            None,
            axum::http::StatusCode::BAD_REQUEST,
            "invalid_query",
        ),
        (
            Method::POST,
            "/api/proxy/rpc?url=https%3A%2F%2Frpc.ankr.com%2Feth&unexpected=1",
            Some(json!({
                "jsonrpc": "2.0",
                "id": 1,
                "method": "eth_chainId",
                "params": []
            })),
            axum::http::StatusCode::BAD_REQUEST,
            "invalid_query",
        ),
        (
            Method::GET,
            "/api/evm/quote?chainId=1&sellToken=0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee&buyToken=0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48&sellAmount=1000000000000000000&taker=0x1111111111111111111111111111111111111111&slippageBps=100&unexpected=1",
            None,
            axum::http::StatusCode::BAD_REQUEST,
            "invalid_query",
        ),
        (
            Method::GET,
            "/api/jupiter/quote?inputMint=So11111111111111111111111111111111111111112&outputMint=EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v&amount=1000000&unexpected=1",
            None,
            axum::http::StatusCode::BAD_REQUEST,
            "invalid_query",
        ),
    ];

    for (method, path, body, expected_status, expected_code) in cases {
        let (status, _headers, payload) =
            json_request_with_headers(app.clone(), method, path, body, &[]).await;
        assert_eq!(status, expected_status, "path={path}");
        assert_eq!(
            payload
                .get("error")
                .and_then(|v| v.get("code"))
                .and_then(|v| v.as_str()),
            Some(expected_code),
            "path={path}"
        );
    }
}

#[tokio::test]
async fn phase2_financial_routes_reject_unknown_json_fields_with_stable_4xx_shape() {
    let _l = global_test_lock().await;
    let app = build_app(test_config()).await.expect("build app");

    let (sol_status, sol_body) = json_request(
        app.clone(),
        Method::POST,
        "/api/solana/amm/swap-tx",
        json!({
            "amountIn": "1000",
            "minOut": "900",
            "direction": "AtoB",
            "userPublicKey": "11111111111111111111111111111111",
            "unexpected": true
        }),
        None,
    )
    .await;
    assert_eq!(sol_status, axum::http::StatusCode::BAD_REQUEST);
    assert_eq!(
        sol_body
            .get("error")
            .and_then(|v| v.get("code"))
            .and_then(|v| v.as_str()),
        Some("invalid_json_body")
    );

    let (tn_status, tn_body) = json_request(
        app,
        Method::POST,
        "/api/testnet-amm/deploy",
        json!({
            "chain": "sepolia",
            "rpcUrl": "https://rpc.ankr.com/eth_sepolia",
            "unexpected": "x"
        }),
        Some("test-admin-token"),
    )
    .await;
    assert_eq!(tn_status, axum::http::StatusCode::BAD_REQUEST);
    assert_eq!(
        tn_body
            .get("error")
            .and_then(|v| v.get("code"))
            .and_then(|v| v.as_str()),
        Some("invalid_json_body")
    );
}
