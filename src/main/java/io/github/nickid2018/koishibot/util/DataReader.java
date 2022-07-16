package io.github.nickid2018.koishibot.util;

import io.github.nickid2018.koishibot.core.ErrorRecord;

import java.io.*;
import java.util.function.Supplier;

public class DataReader<E> {

    private final File file;
    private final Supplier<E> empty;

    private E data;

    public DataReader(File file, Supplier<E> empty) {
        this.file = file;
        this.empty = empty;
    }

    @SuppressWarnings("unchecked")
    public E getData() throws IOException {
        if (data != null)
            return data;
        if (!file.exists()) {
            if (!file.getParentFile().isDirectory())
                file.getParentFile().mkdirs();
            file.createNewFile();
            return data = empty.get();
        }
        try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(file))) {
            return data = (E) is.readObject();
        } catch (Exception e) {
            ErrorRecord.enqueueError("data." + file.getName(), e);
            data = empty.get();
            saveData();
            return data;
        }
    }

    public void saveData() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(data);
        }
    }
}
