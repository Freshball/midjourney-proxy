package com.github.novicezk.midjourney.service.store;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.novicezk.midjourney.service.TaskStoreService;
import com.github.novicezk.midjourney.support.Task;
import com.github.novicezk.midjourney.support.TaskCondition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.util.List;
import java.util.Random;


public class InMemoryTaskStoreServiceImpl implements TaskStoreService {
	private final TimedCache<String, Task> taskMap;

	public InMemoryTaskStoreServiceImpl(Duration timeout) {
		this.taskMap = CacheUtil.newTimedCache(timeout.toMillis());
	}

	@Override
	public void save(Task task) {
		this.taskMap.put(task.getId(), task);
	}

	@Override
	public void delete(String key) {
		this.taskMap.remove(key);
	}

	/**
	 * 获取指定位数随机字符串
	 */
	public static String getRandomString(Integer num) {
		String base = "abcdefghijklmnopqrstuvwxyz0123456789";
		Random random = new Random();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < num; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		return sb.toString();
	}

	// 生产文件新的URL
	private String generateKey(String originalFilename) {
		int index = originalFilename.lastIndexOf('.');
		String suffix = originalFilename.substring(index);
		String key = getRandomString(20) + suffix;
		return key;
	}

	@Override
	public Task get(String key) {
		Task task = this.taskMap.get(key);
		if(StrUtil.isNotBlank(task.getImageUrl())){
			InputStream is = null;
			FileOutputStream os = null;
			try {
				// 构造URL
				URL url = new URL(task.getImageUrl());
				// 打开连接
				URLConnection con = url.openConnection();
				// 输入流
				is = con.getInputStream();
				// 1K的数据缓冲
				byte[] bs = new byte[1024];
				// 读取到的数据长度
				int len;
				// 输出的文件流
				String filename = System.getProperty("os.name").toLowerCase().contains("win") ? System.getProperty("user.home") + "\\Desktop\\temp.jpg" : "/home/file/" + generateKey(task.getImageUrl());
				File file = new File(filename);
				os = new FileOutputStream(file, true);
				// 开始读取
				while ((len = is.read(bs)) != -1) {
					os.write(bs, 0, len);
				}
				task.setImageUrl(filename);
//				return filename;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				// 完毕，关闭所有链接
				try {
					if (null != os) {
						os.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					if (null != is) {
						is.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//从微信下载图片时如果没有id对应的图片则下载一个空图片，不会存在返回为null的情况
			return null;
		}
		return task;
	}

	@Override
	public List<Task> list() {
		return ListUtil.toList(this.taskMap.iterator());
	}

	@Override
	public List<Task> list(TaskCondition condition) {
		return StreamUtil.of(this.taskMap.iterator()).filter(condition).toList();
	}

	@Override
	public Task findOne(TaskCondition condition) {
		return StreamUtil.of(this.taskMap.iterator()).filter(condition).findFirst().orElse(null);
	}

}
