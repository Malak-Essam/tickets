package com.example.tickets.mapper;

import org.springframework.data.domain.Page;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.example.tickets.domain.Event;
import com.example.tickets.domain.TicketType;
import com.example.tickets.dto.request.CreateEventRequest;
import com.example.tickets.dto.request.CreateTicketTypeRequest;
import com.example.tickets.dto.response.EventResponse;
import com.example.tickets.dto.response.PageResponse;
import com.example.tickets.dto.response.TicketTypeResponse;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ticketTypes", ignore = true)
    Event toEvent(CreateEventRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    TicketType toTicketType(CreateTicketTypeRequest request);

    @Mapping(target = "organizerId", source = "organizer.id")
    EventResponse toResponse(Event event);

    TicketTypeResponse toResponse(TicketType ticketType);

    default PageResponse<EventResponse> toPageResponse(Page<Event> page) {
        return new PageResponse<>(
            page.getContent().stream().map(this::toResponse).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages());
    }
}
