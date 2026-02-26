use axum::{
    extract::{Request, State},
    middleware::Next,
    response::Response,
};

use crate::app::{rate_limit_middleware_impl, AppState};

pub async fn rate_limit_middleware(
    State(st): State<AppState>,
    req: Request,
    next: Next,
) -> Response {
    rate_limit_middleware_impl(State(st), req, next).await
}
