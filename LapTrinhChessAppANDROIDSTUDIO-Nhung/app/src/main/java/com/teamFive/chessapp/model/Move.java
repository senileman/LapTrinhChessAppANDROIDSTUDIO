package com.teamFive.chessapp.model;

public class Move {
    public Position from;
    public Position to;

    public Move(Position from, Position to) {
        this.from = from;
        this.to = to;
    }
}