package org.mitre.synthea.codebase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.mitre.synthea.codebase.generated.Codebase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

/**
 * CodeMapBuilder is a singleton class responsible for building and retrieving 
 * a CodeMap object. It offers functionality to load a CodeMap either from 
 * an XML file or from a classpath resource. The class supports multiple ways 
 * to obtain the CodeMap, including reading it from a file system, from resources 
 * inside a JAR, or from an XML string.
 * 
 * Key methods include:
 * - getCodeMap(InputStream inputStream): Loads a CodeMap from an input stream.
 * - getCodeMap(String codebaseXml): Loads a CodeMap from a provided XML string.
 * - findAndReadCodeMapIntoMemory(String path): Attempts to load the CodeMap 
 *   from a file in the specified path, checking the local directory and classpath.
 * - getDefaultCodeMap(): Returns the default CodeMap, which is pre-built.
 * - getCompiledCodeMap(): Loads and returns the compiled CodeMap from a file.
 * - getCodeMapFromClasspathResource(String resourcePath): Retrieves a CodeMap 
 *   from a classpath resource.
 * - getCodeMapFromSameDirAsJar(String resourcePath): Retrieves a CodeMap from 
 *   a file located in the same directory as the JAR file.
 * 
 * This class is designed to centralize the logic for obtaining and caching 
 * CodeMap objects, simplifying the management of codebase configurations within 
 * the system.
 */


public enum CodeMapBuilder {
  INSTANCE;

  private CodeMap preBuilt;

  private static final Logger logger = LoggerFactory
      .getLogger(CodeMapBuilder.class);

  public CodeMap getCodeMap(InputStream inputStream) {
    logger.trace("input stream: " + inputStream);
    if (inputStream == null) {
      throw new IllegalArgumentException(
          "No file provided for CodeMap:  Verify that you are building a CodeMap from a file that exists.");
    }

    JAXBContext jaxbContext;
    try {

      jaxbContext = JAXBContext.newInstance(Codebase.class);
      Unmarshaller jaxbUM = jaxbContext.createUnmarshaller();
      Codebase hcp = (Codebase) jaxbUM.unmarshal(inputStream);
      CodeMap cm = new CodeMap(hcp);
      this.preBuilt = cm;
      return cm;
    } catch (JAXBException e) {
      throw new RuntimeException("Could not marshall the codemap", e);
    }
  }

  public CodeMap getCodeMap(String codebaseXml) {
    InputStream is = new ByteArrayInputStream(codebaseXml.getBytes());
    return getCodeMap(is);
  }

  CodeMap findAndReadCodeMapIntoMemory(String path) {
    CodeMap cm;
    String file = path;
    InputStream is;
    try {
      is = getCodeMapFromSameDirAsJar(file);
      logger.warn("Using Compiled.xml from directory");
    } catch (FileNotFoundException e) {
      logger.warn("Compiled.xml not found in directory with jar.  checking classpath");
      is = getCodeMapFromClasspathResource("/" + file);
      if (is != null) {
        logger.warn("Using Compiled.xml from classpath (resources folder in jar)");
      }
    }
    if (is != null) {
      cm = getCodeMap(is);
    } else {
      throw new IllegalArgumentException(
          "You cannot build a CodeMap if the input stream is null.  Verify that you are building an input stream from a file that exists. ");
    }
    return cm;
  }


  public CodeMap getDefaultCodeMap() {
    return getCompiledCodeMap();
  }


  public CodeMap getDefaultCodeMap(String path) {
    return getCompiledCodeMap(path);
  }

  public CodeMap getCompiledCodeMap() {
    if (preBuilt == null) {
      this.preBuilt = findAndReadCodeMapIntoMemory("./src/test/resources/Compiled.xml");
    }
    return preBuilt;
  }


    /**
   * Retrieves the compiled CodeMap, loading it into memory if not already loaded.
   * If the CodeMap has not been previously loaded, it reads from the specified file path.
   *
   * @param path the file path to the compiled CodeMap XML file
   * @return the loaded CodeMap object
   */

  public CodeMap getCompiledCodeMap(String path) {
    if (preBuilt == null) {
      this.preBuilt = findAndReadCodeMapIntoMemory(path);
    }
    return preBuilt;
  }

  public InputStream getCodeMapFromClasspathResource(String resourcePath) {
    logger.warn("Getting resource [" + resourcePath + "]" );
    InputStream is = Object.class.getResourceAsStream(resourcePath);
    if (is == null) {
      is = getClass().getClassLoader().getResourceAsStream(resourcePath);
    }
    return is;
  }

  public InputStream getCodeMapFromSameDirAsJar(String resourcePath) throws FileNotFoundException {
    logger.warn("Current dir: " + new File("").getAbsolutePath());
    File f = new File(resourcePath);
    logger.warn("Looking in: " + f.getAbsolutePath() + " for file");
    return new FileInputStream(f);
  }
}
