package com.bank_simulator.demo.mapper;

import com.bank_simulator.demo.model.Card;
import com.bank_simulator.demo.model.CardInfo;
import com.bank_simulator.demo.model.CardsResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardMapper {

    @Mapping(source = "number", target = "cardNumber")
    CardInfo toCardInfo(Card card);

    List<CardInfo> toCardInfoList(List<Card> cards);

    default CardsResponse toCardsResponse(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            CardsResponse response = new CardsResponse();
            response.setItems(List.of());
            return response;
        }
        CardsResponse response = new CardsResponse();
        response.setItems(toCardInfoList(cards));
        return response;
    }
}
