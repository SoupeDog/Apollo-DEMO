package org.xavier.demo.controller;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.xavier.demo.config.DateBaseProperties;
import org.xavier.demo.config.DynamicDataSource;
import org.xavier.demo.dao.UserMapper;
import org.xavier.demo.domain.po.User;

import java.sql.SQLException;

/**
 * 描述信息：<br/>
 *
 * @author Xavier
 * @version 1.0
 * @date 2020/11/2
 * @since Jdk 1.8
 */
@RestController
public class MainController {
    @Autowired
    UserMapper userMapper;
    @Autowired
    DynamicDataSource dynamicDataSource;
    @Autowired
    SqlSessionFactory sqlSessionFactory;

    @GetMapping("/")
    public Object query() {
        return userMapper.queryUserById(1L);
    }

    @GetMapping("/block")
    public Object queryB() throws InterruptedException {
        SqlSession sqlSession = sqlSessionFactory.openSession(false);
        UserMapper currentMapper = sqlSession.getMapper(UserMapper.class);
        User user = currentMapper.queryUserById(1L);
        sqlSession.commit();
        sqlSession.close();
        return user;
    }

    @PutMapping("/datasource")
    public Object updateDataSource(@RequestBody DateBaseProperties dateBaseProperties) throws SQLException {
        DruidDataSource old = dynamicDataSource.refreshDataSource(dateBaseProperties);
        old.close();
        old = null;
        // 建议 JVM 进行垃圾回收
        System.gc();
        // 强制调用已经失去引用的对象的finalize方法
        System.runFinalization();
        return "success";
    }

}