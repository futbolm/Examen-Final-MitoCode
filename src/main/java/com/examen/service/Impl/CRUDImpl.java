package com.examen.service.Impl;

import com.examen.exception.ModelNotFountException;
import com.examen.repo.IGenericRepo;
import com.examen.service.ICRUD;

import java.lang.reflect.Method;
import java.util.List;

public abstract class CRUDImpl<T,ID> implements ICRUD<T,ID> {

    protected abstract IGenericRepo<T,ID> getRepo();

    @Override
    public T save(T t) {
        return getRepo().save(t);
    }

    @Override
    public T update(ID id, T t) throws Exception {
        T _ = findById(id);

        String className = t.getClass().getSimpleName();

        String methodName = "setId" + className;

        Method setIdMethod = t.getClass().getMethod(methodName, id.getClass());

        setIdMethod.invoke(t,id);
        return  getRepo().save(t);
    }

    @Override
    public List<T> findAll() {
        return getRepo().findAll();
    }

    @Override
    public T findById(ID id) {
        return getRepo().findById(id).orElseThrow(() -> new ModelNotFountException("ID NOT FOUND : " + id));
    }

    @Override
    public void delete(ID id) {
        T _ = findById(id);
        getRepo().deleteById(id);
    }
}
