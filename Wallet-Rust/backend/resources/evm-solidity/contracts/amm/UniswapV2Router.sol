// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

interface IERC20LiteRouter {
    function transferFrom(address from, address to, uint256 amount) external returns (bool);
}

import "./UniswapV2Factory.sol";
import "./UniswapV2Pair.sol";

contract UniswapV2Router {
    UniswapV2Factory public factory;

    constructor(address _factory) {
        require(_factory != address(0), "Factory required");
        factory = UniswapV2Factory(_factory);
    }

    function getReserves(address tokenA, address tokenB) public view returns (uint256 reserveA, uint256 reserveB) {
        address pair = factory.getPair(tokenA, tokenB);
        require(pair != address(0), "Pair not found");
        (uint112 reserve0, uint112 reserve1) = UniswapV2Pair(pair).getReserves();
        (address token0,) = tokenA < tokenB ? (tokenA, tokenB) : (tokenB, tokenA);
        if (tokenA == token0) {
            reserveA = reserve0;
            reserveB = reserve1;
        } else {
            reserveA = reserve1;
            reserveB = reserve0;
        }
    }

    function quote(uint256 amountA, uint256 reserveA, uint256 reserveB) public pure returns (uint256 amountB) {
        require(amountA > 0, "Insufficient amount");
        require(reserveA > 0 && reserveB > 0, "Insufficient liquidity");
        amountB = (amountA * reserveB) / reserveA;
    }

    function getAmountOut(uint256 amountIn, uint256 reserveIn, uint256 reserveOut) public pure returns (uint256 amountOut) {
        require(amountIn > 0, "Insufficient input");
        require(reserveIn > 0 && reserveOut > 0, "Insufficient liquidity");
        uint256 amountInWithFee = amountIn * 997;
        uint256 numerator = amountInWithFee * reserveOut;
        uint256 denominator = (reserveIn * 1000) + amountInWithFee;
        amountOut = numerator / denominator;
    }

    function getAmountsOut(uint256 amountIn, address[] memory path) public view returns (uint256[] memory amounts) {
        require(path.length >= 2, "Invalid path");
        amounts = new uint256[](path.length);
        amounts[0] = amountIn;
        for (uint256 i = 0; i < path.length - 1; i++) {
            (uint256 reserveIn, uint256 reserveOut) = getReserves(path[i], path[i + 1]);
            amounts[i + 1] = getAmountOut(amounts[i], reserveIn, reserveOut);
        }
    }

    function addLiquidity(
        address tokenA,
        address tokenB,
        uint256 amountADesired,
        uint256 amountBDesired,
        uint256 amountAMin,
        uint256 amountBMin,
        address to,
        uint256 deadline
    ) external returns (uint256 amountA, uint256 amountB, uint256 liquidity) {
        require(block.timestamp <= deadline, "Expired");
        address pair = factory.getPair(tokenA, tokenB);
        if (pair == address(0)) {
            pair = factory.createPair(tokenA, tokenB);
            amountA = amountADesired;
            amountB = amountBDesired;
        } else {
            (uint256 reserveA, uint256 reserveB) = getReserves(tokenA, tokenB);
            if (reserveA == 0 && reserveB == 0) {
                amountA = amountADesired;
                amountB = amountBDesired;
            } else {
                uint256 amountBOptimal = quote(amountADesired, reserveA, reserveB);
                if (amountBOptimal <= amountBDesired) {
                    require(amountBOptimal >= amountBMin, "Insufficient B amount");
                    amountA = amountADesired;
                    amountB = amountBOptimal;
                } else {
                    uint256 amountAOptimal = quote(amountBDesired, reserveB, reserveA);
                    require(amountAOptimal <= amountADesired, "Insufficient A amount");
                    require(amountAOptimal >= amountAMin, "Insufficient A amount");
                    amountA = amountAOptimal;
                    amountB = amountBDesired;
                }
            }
        }

        IERC20LiteRouter(tokenA).transferFrom(msg.sender, pair, amountA);
        IERC20LiteRouter(tokenB).transferFrom(msg.sender, pair, amountB);
        liquidity = UniswapV2Pair(pair).mint(to);
    }

    function swapExactTokensForTokens(
        uint256 amountIn,
        uint256 amountOutMin,
        address[] calldata path,
        address to,
        uint256 deadline
    ) external returns (uint256[] memory amounts) {
        require(block.timestamp <= deadline, "Expired");
        amounts = getAmountsOut(amountIn, path);
        require(amounts[amounts.length - 1] >= amountOutMin, "Insufficient output");

        address firstPair = factory.getPair(path[0], path[1]);
        require(firstPair != address(0), "Pair not found");
        IERC20LiteRouter(path[0]).transferFrom(msg.sender, firstPair, amounts[0]);
        _swap(amounts, path, to);
    }

    function _swap(uint256[] memory amounts, address[] memory path, address _to) internal {
        for (uint256 i = 0; i < path.length - 1; i++) {
            (address input, address output) = (path[i], path[i + 1]);
            address pair = factory.getPair(input, output);
            require(pair != address(0), "Pair not found");
            (address token0,) = input < output ? (input, output) : (output, input);
            uint256 amountOut = amounts[i + 1];
            (uint256 amount0Out, uint256 amount1Out) = input == token0
                ? (uint256(0), amountOut)
                : (amountOut, uint256(0));
            address to = i < path.length - 2 ? factory.getPair(output, path[i + 2]) : _to;
            UniswapV2Pair(pair).swap(amount0Out, amount1Out, to);
        }
    }
}
