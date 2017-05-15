package org.squiddev.cctweaks.lua;

import javax.annotation.Nonnull;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper methods for various things
 */
public class ThreadBuilder {
	public static int LOW_PRIORITY = Thread.MIN_PRIORITY + (Thread.NORM_PRIORITY - Thread.MIN_PRIORITY) / 2;
	public static int NORM_PRIORITY = Thread.NORM_PRIORITY;

	public static ThreadPoolExecutor createThread(String name, int minThreads, int maxThreads, final int priority) {
		return new ThreadPoolExecutor(
			minThreads, maxThreads,
			60L, TimeUnit.SECONDS,
			new SynchronousQueue<Runnable>(),
			getFactory(name, priority)
		);
	}

	public static ThreadFactory getFactory(String name, final int priority) {
		final String prefix = "CCTweaks-" + name + "-";
		final AtomicInteger counter = new AtomicInteger(1);

		SecurityManager manager = System.getSecurityManager();
		final ThreadGroup group = manager == null ? Thread.currentThread().getThreadGroup() : manager.getThreadGroup();

		return new ThreadFactory() {
			@Override
			public Thread newThread(@Nonnull Runnable runnable) {
				Thread thread = new Thread(group, runnable, prefix + counter.getAndIncrement());
				if (!thread.isDaemon()) thread.setDaemon(true);
				if (thread.getPriority() != priority) thread.setPriority(priority);

				return thread;
			}
		};
	}

	public static ThreadPoolExecutor createThread(String name, int threads, int priority) {
		return createThread(name, threads, threads, priority);
	}
}
