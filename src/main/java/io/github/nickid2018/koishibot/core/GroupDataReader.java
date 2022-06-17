package io.github.nickid2018.koishibot.core;

import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.util.BiConsumerE;
import io.github.nickid2018.koishibot.util.FunctionE;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class GroupDataReader<E> {

    private final String name;
    private final FunctionE<InputStream, E> reader;
    private final BiConsumerE<OutputStream, E> writer;
    private final Supplier<E> empty;
    protected final File folder;

    protected final Map<Long, E> data = new HashMap<>();

    public GroupDataReader(String name, FunctionE<InputStream, E> reader,
                           BiConsumerE<OutputStream, E> writer, Supplier<E> empty) {
       this.name = name;
       this.reader = reader;
       this.writer = writer;
       this.empty = empty;
       folder = new File(KoishiBotMain.INSTANCE.getDataFolder(), name);
    }

    public E getData(long group) {
        return data.computeIfAbsent(group, g -> {
            File file = new File(folder, g + "");
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

    public void putData(long group, E data) throws Exception {
        this.data.put(group, data);
        writer.accept(new FileOutputStream(new File(folder, group + "")), data);
    }

    public void updateData(long group, Function<E, E> changer) throws Exception {
        putData(group, changer.apply(getData(group)));
    }

    public File getFolder() {
        return folder;
    }

    public Set<Long> getGroups() {
        return new HashSet<>(data.keySet());
    }
}
