package ai.dataeng.sqml.tree.name;

import ai.dataeng.sqml.tree.QualifiedName;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

@Value
public class NamePath implements Iterable<Name>, Serializable, Comparable<NamePath> {

    public static NamePath ROOT = new NamePath(new Name[0]);

    private final Name[] names;

    private NamePath(@NonNull Name... names) {
        this.names = names;
    }

    public static NamePath of(@NonNull Name... names) {
        return new NamePath(names);
    }
    public static NamePath of(@NonNull List<Name> names) {
        return new NamePath(names.toArray(new Name[0]));
    }

    public NamePath resolve(@NonNull Name name) {
        Name[] newnames = Arrays.copyOf(names,names.length+1);
        newnames[names.length] = name;
        return new NamePath(newnames);
    }

    public NamePath resolve(@NonNull NamePath sub) {
        Name[] newnames = Arrays.copyOf(names,names.length+sub.names.length);
        System.arraycopy(sub.names, 0, newnames, names.length, sub.names.length);
        return new NamePath(newnames);
    }

    public NamePath prefix(int depth) {
        if (depth==0) return ROOT;
        Name[] newnames = Arrays.copyOf(names,depth);
        return new NamePath(newnames);
    }

    /**
     * @deprecated Use {@link #getLength()} instead
     * @return
     */
    public int getNumComponents() {
        return names.length;
    }

    public int getLength() {
        return names.length;
    }

    public Name get(int index) {
        Preconditions.checkArgument(index>=0 && index<getNumComponents());
        return names[index];
    }

    public Optional<Name> getOptional(int index) {
        if(index>=0 && index<getNumComponents()) {
            return Optional.of(names[index]);
        } else {
            return Optional.empty();
        }
    }

    public Name getLast() {
        Preconditions.checkArgument(names.length>0);
        return names[names.length-1];
    }

    @Override
    public String toString() {
        return toString('.');
    }

    public String toString(char separator) {
        if (names.length==0) return "/";
        return StringUtils.join(names,separator);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(names);
    }

    @Override
    public boolean equals(Object other) {
        if (this==other) return true;
        else if (other==null) return false;
        else if (!(other instanceof NamePath))return false;
        NamePath o = (NamePath) other;
        return Arrays.equals(names,o.names);
    }


    @Override
    public Iterator<Name> iterator() {
        return Iterators.forArray(names);
    }

    @Override
    public int compareTo(NamePath o) {
        return Arrays.compare(names,o.names);
    }

    public Name toName() {
        return null;
    }
    public Optional<NamePath> getPrefix() {

        if (names.length <= 1) {
            return Optional.empty();
        }

        Name[] newNames = Arrays.copyOfRange(names, 0, names.length - 1);
        return Optional.of(NamePath.of(newNames));
    }

    public String getDisplay() {
        return Arrays.stream(names)
            .map(e->e.getDisplay())
            .collect(Collectors.joining("."));
    }

    public NamePath popFirst() {
        Name[] newNames = Arrays.copyOfRange(names, 1, names.length);
        return NamePath.of(newNames);
    }

    public Name getFirst() {
        return names[0];
    }

//    public NamePath concat(NamePath namePath) {
//        Name[] newNames = Arrays.copyOfRange(names, 0, names.length - 1);
//        return Optional.of(NamePath.of(newNames));
//        return null;
//    }
}
