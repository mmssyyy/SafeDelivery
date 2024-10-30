package com.example.safedelivery;

import com.naver.maps.geometry.LatLng;

public class AddressItem {
    String address;
    LatLng latLng;

    AddressItem(String address, LatLng latLng) {
        this.address = address;
        this.latLng = latLng;
    }
}