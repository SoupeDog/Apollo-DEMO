package org.xavier.demo.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.xavier.demo.domain.po.User;

@Mapper
public interface UserMapper {

    @Select("Select * From User Where id=#{id}")
    User queryUserById(@Param("id") Long id);
}