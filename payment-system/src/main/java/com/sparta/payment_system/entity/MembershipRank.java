package com.sparta.payment_system.entity;


public enum MembershipRank {
    NORMAL, VIP, VVIP;


    public static MembershipRank fromTotalPaid(long totalPaid) {
        if (totalPaid >= 150_000) return VVIP;
        if (totalPaid >= 100_000) return VIP;
        return NORMAL;
    }
}
