package io.github.nickid2018.koishibot.util;

import io.github.nickid2018.koishibot.core.ErrorRecord;
import org.apache.commons.io.filefilter.SuffixFileFilter;

import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class GroupDataReader<E> {

    private final String name;
    private final FunctionE<InputStream, E> reader;
    private final BiConsumerE<OutputStream, E> writer;
    private final Supplier<E> empty;
    protected final File folder;

    protected final Map<String, E> data = new ConcurrentHashMap<>();

    public GroupDataReader(String name, FunctionE<InputStream, E> reader,
                           BiConsumerE<OutputStream, E> writer, Supplier<E> empty) {
       this.name = name;
       this.reader = reader;
       this.writer = writer;
       this.empty = empty;
       folder = new File("data", name);
       if (!folder.isDirectory())
           folder.mkdirs();
    }

    public void loadAll() {
        File[] files = folder.listFiles((FileFilter) new SuffixFileFilter("gpd"));
        if (files == null)
            return;
        for (File file : files) {
            String name = file.getName();
            getData(name.substring(0, name.lastIndexOf('.')));
        }
    }

    public E getData(String group) {
        return data.computeIfAbsent(group, g -> {
            File file = new File(folder, g + ".gpd");
            if (!file.exists())
                return empty.get();
            try (InputStream is = new FileInputStream(file)) {
                return reader.apply(is);
            } catch (Exception e) {
                ErrorRecord.enqueueError("group.data." + name, e);
                return empty.get();
            }
        });
    }

    public void putData(String group, E data) throws Exception {
        this.data.put(group, data);
        File file = new File(folder, group + ".gpd");
        if (!file.exists())
            file.createNewFile();
        writer.accept(new FileOutputStream(file), data);
    }

    public void updateData(String group, Function<E, E> changer) throws Exception {
        putData(group, changer.apply(getData(group)));
    }

    public File getFolder() {
        return folder;
    }

    public Set<String> getGroups() {
        return new HashSet<>(data.keySet());
    }
}
