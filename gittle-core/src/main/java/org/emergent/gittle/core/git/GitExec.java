package org.emergent.gittle.core.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.emergent.gittle.core.GittleException;

import java.io.File;
import java.io.IOException;

public class GitExec {

  public static <R> R execOp(File basePath, ExecFunction<Git, R> work) throws GittleException {
    try (Repository repo = getRepository(basePath);
         Git git = new Git(repo)) {
      return work.apply(git);
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Exception e) {
      throw new GittleException(e);
    }
  }

  public static void execOp(File basePath, ExecConsumer<Git> work) throws GittleException {
    try (Repository repo = getRepository(basePath);
         Git git = new Git(repo)) {
      work.accept(git);
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Exception e) {
      throw new GittleException(e);
    }
  }

  public static String findGitDir(File basePath) {
    try (Repository repo = getRepository(basePath, true)) {
      return repo.getDirectory().getAbsolutePath();
    } catch (Exception e) {
      throw new GittleException(e);
    }
  }

  public static Repository getRepository(File basePath) throws IOException {
    return getRepository(basePath, false);
  }

  public static Repository getRepository(File basePath, boolean mustExist) throws IOException {
    return new FileRepositoryBuilder()
        .readEnvironment()
        .findGitDir(normalize(basePath))
        .setMustExist(mustExist)
        .build();
  }

  private static File normalize(File file) {
    return file != null ? file.getAbsoluteFile() : null;
  }

  @FunctionalInterface
  public interface ExecFunction<T, R> {
    R apply(T t) throws Exception;
  }

  @FunctionalInterface
  public interface ExecConsumer<T> {
    void accept(T t) throws Exception;
  }
}
