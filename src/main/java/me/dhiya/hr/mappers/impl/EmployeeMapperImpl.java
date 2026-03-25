package me.dhiya.hr.mappers.impl;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.dto.employee.response.EmployeeDto;
import me.dhiya.hr.mappers.Mapper;

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
