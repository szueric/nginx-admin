package com.jslsolucoes.nginx.admin.agent.resource.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import com.jslsolucoes.file.system.FileSystemBuilder;
import com.jslsolucoes.nginx.admin.agent.config.Configuration;
import com.jslsolucoes.nginx.admin.agent.resource.impl.info.NginxInfo;
import com.jslsolucoes.nginx.admin.agent.resource.impl.info.NginxInfoDiscover;
import com.jslsolucoes.nginx.admin.agent.resource.impl.os.OperationalSystem;
import com.jslsolucoes.nginx.admin.agent.resource.impl.os.OperationalSystemInfo;
import com.jslsolucoes.nginx.admin.agent.resource.impl.status.NginxStatus;
import com.jslsolucoes.nginx.admin.agent.resource.impl.status.NginxStatusDiscover;
import com.jslsolucoes.template.TemplateBuilder;

@RequestScoped
public class NginxAdminResourceImpl {

	private NginxInfoDiscover nginxInfoDiscover;
	private NginxStatusDiscover nginxStatusDiscover;
	private Configuration configuration;

	@Deprecated
	public NginxAdminResourceImpl() {

	}

	@Inject
	public NginxAdminResourceImpl(NginxInfoDiscover nginxInfoDiscover, NginxStatusDiscover nginxStatusDiscover,
			Configuration configuration) {
		this.configuration = configuration;
		this.nginxInfoDiscover = nginxInfoDiscover;
		this.nginxStatusDiscover = nginxStatusDiscover;
	}

	public NginxOperationResult configure(Integer maxPostSize, Boolean gzip) {
		try {
			applyFs();
			applyTemplate(maxPostSize, gzip);
			return new NginxOperationResult(NginxOperationResultType.SUCCESS);
		} catch (Exception exception) {
			return new NginxOperationResult(NginxOperationResultType.ERROR, exception);
		}
	}

	private void applyTemplate(Integer maxPostSize, Boolean gzip) {
		String settings = settings();
		try(FileWriter fileWriter = new FileWriter(new File(virtualHost(), "root.conf"))) {
			TemplateBuilder.newBuilder()
				.withClasspathTemplate("/template/nginx/dynamic", "root.tpl")
			.withData("settings", settings).withOutput(fileWriter).process();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		try(FileWriter fileWriter = new FileWriter(new File(settings, "nginx.conf"))) {
			TemplateBuilder.newBuilder()
			.withClasspathTemplate("/template/nginx/dynamic", "nginx.tpl").withData("settings", settings)
			.withData("gzip", gzip).withData("maxPostSize", maxPostSize)
			.withOutput(fileWriter).process();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

	private File virtualHost() {
		return new File(settings(), "virtual-host");
	}

	private void applyFs() {
		String setting = settings();
		FileSystemBuilder.newBuilder().create().withDestination(setting).execute().end().copy()
				.withClasspathResource("/template/nginx/fixed").withDestination(setting).execute().end();
	}

	private String settings() {
		return configuration.getNginx().getSetting();
	}

	public OperationalSystemInfo os() {
		return OperationalSystem.info();
	}

	public NginxInfo info() {
		return nginxInfoDiscover.info();
	}

	public NginxStatus status() {
		return nginxStatusDiscover.status();
	}

}
