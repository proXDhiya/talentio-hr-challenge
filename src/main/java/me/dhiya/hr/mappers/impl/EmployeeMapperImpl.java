package me.dhiya.hr.mappers.impl;

import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.dto.response.EmployeeDto;
import me.dhiya.hr.mappers.Mapper;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapperImpl implements Mapper<EmployeeEntity, EmployeeDto> {

    private final ModelMapper modelMapper;

    public EmployeeMapperImpl(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public EmployeeDto mapTo(EmployeeEntity entity) {
        return modelMapper.map(entity, EmployeeDto.class);
    }

    @Override
    public EmployeeEntity mapFrom(EmployeeDto dto) {
        return modelMapper.map(dto, EmployeeEntity.class);
    }
}
