use axum::{
    extract::{Request, State},
    middleware::Next,
    response::Response,
};

use crate::app::{request_context_middleware_impl, AppState};

pub async fn request_context_middleware(
    State(st): State<AppState>,
    req: Request,
    next: Next,
) -> Response {
    request_context_middleware_impl(State(st), req, next).await
}
