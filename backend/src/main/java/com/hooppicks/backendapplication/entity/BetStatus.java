package com.hooppicks.backendapplication.entity;

public enum BetStatus {
    // CASHED_OUT : encaissé avant la fin (CashOutService), le gain compté est la valeur encaissée.
    PENDING, WON, LOST, VOID, CASHED_OUT
}
