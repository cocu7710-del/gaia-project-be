package com.gaiaproject.dto.response;

import com.gaiaproject.domain.entity.booster.GameBoosterOffer;

import java.util.UUID;

public record BoosterOfferResponse(
        UUID id,
        String boosterCode,
        int position,
        Integer pickedBySeatNo
) {
    public static BoosterOfferResponse from(GameBoosterOffer offer) {
        return new BoosterOfferResponse(
                offer.getId(),
                offer.getBoosterCode(),
                offer.getPosition(),
                offer.getPickedBySeatNo()
        );
    }
}
