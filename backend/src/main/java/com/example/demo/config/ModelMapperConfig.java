package com.example.demo.config;

import com.example.demo.dto.ItineraryListDTO;
import com.example.demo.model.ItineraryList;
import com.example.demo.model.Trip;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // Configurazione specifica per ItineraryList -> ItineraryListDTO
        modelMapper.typeMap(ItineraryList.class, ItineraryListDTO.class).addMappings(mapper -> {
            // Mappiamo manualmente la conversione da Set<Trip> a Set<Long>
            mapper.using(ctx -> {
                Set<Trip> source = (Set<Trip>) ctx.getSource();
                return source != null ? source.stream().map(Trip::getId).collect(Collectors.toSet()) : null;
            }).map(ItineraryList::getTrips, ItineraryListDTO::setTripIds);
        });

        return modelMapper;
    }
}