package com.example.vmserver.specifications;


import org.springframework.data.jpa.domain.Specification;
import com.example.vmserver.model.VMStation;

public class VMStationSpecifications {

    public static Specification<VMStation> hasIp(String ip) {
    return (root, query, criteriaBuilder) -> 
        ip == null ? null : criteriaBuilder.equal(root.get("ip"), ip);
    }

    /*
     * тут переделать под логин станции
     */
    private static Specification<VMStation> titleLogin(String login){
        return (root, query, criteriaBilder) -> {
            if(login == null || login.trim().isEmpty()){
                return null;
            }
            return criteriaBilder.like(criteriaBilder.lower(root.get("login")), "%" + login.trim().toLowerCase() + "%");
        };
    }

    /*
     * минимальный порт
     */
    private static Specification<VMStation> portGreater(Integer portMin){
        return (root, query, criteriaBilder) -> {
            if(portMin == null){
                return null;
            }
            return criteriaBilder.greaterThanOrEqualTo(root.get("port"), portMin);
        };
    }

    /*
     * максимальный порт
     */
    private static Specification<VMStation> portLess(Integer portMax){
        return (root, query, criteriaBilder) -> {
            if(portMax == null){
                return null;
            }
            return criteriaBilder.lessThanOrEqualTo(root.get("port"), portMax);
        };
    }

    public static Specification<VMStation> filter(
        String login,
        Integer min,
        Integer max)
    {
        return Specification.allOf(titleLogin(login), portGreater(min), portLess(max));
    }
}
