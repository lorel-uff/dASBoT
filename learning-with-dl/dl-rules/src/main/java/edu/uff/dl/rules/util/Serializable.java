/*
 * UFF Project Semantic Learning
 */
package edu.uff.dl.rules.util;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Interface to describe a class that can be serialized into a file.
 *
 * @author Victor Guimarães.
 * @param <T> The type of the class that implements this interface.
 */
public interface Serializable<T> {

    /**
     * Method to serialize the class into a file.
     *
     * @param file the file.
     * @throws FileNotFoundException in case something goes wrong with the file.
     */
    public void serialize(File file) throws FileNotFoundException;

    /**
     * Method to deserialize the class from a file.
     *
     * @param file the file.
     * @return the deserialized class.
     * @throws FileNotFoundException in case something goes wrong with the file.
     */
    public T deserialize(File file) throws FileNotFoundException;

}
