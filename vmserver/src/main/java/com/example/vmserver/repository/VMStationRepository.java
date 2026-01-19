package com.example.vmserver.repository;

import com.example.vmserver.enums.VMState;
import com.example.vmserver.model.VMStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VMStationRepository extends JpaRepository<VMStation, Long>, JpaSpecificationExecutor<VMStation> {
    //Получить станцию по ip
    VMStation findByIp(String ip);

    // Подсчитать количество станций по статусу
    long countByState(VMState state);
}
