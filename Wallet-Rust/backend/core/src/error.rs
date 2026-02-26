use thiserror::Error;

#[derive(Debug, Error)]
pub enum CoreError {
    #[error("unauthorized")]
    Unauthorized,

    #[error("forbidden")]
    Forbidden,

    #[error("bad request: {0}")]
    BadRequest(&'static str),

    #[error("internal error")]
    Internal,
}
