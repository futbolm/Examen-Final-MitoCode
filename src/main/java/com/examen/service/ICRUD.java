package com.examen.service;

import java.util.List;

public interface ICRUD <T,ID>{
    T save (T t);
    T update (ID id , T t) throws Exception;
    List<T> findAll();
    T findById(ID id);
    void delete(ID id);

}
