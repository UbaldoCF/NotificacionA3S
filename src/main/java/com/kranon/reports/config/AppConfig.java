package com.kranon.reports.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.kranon.reports.service.FileProcessor;

@Configuration
public class AppConfig {

    @Bean
    FileProcessor fileProcessor() {
        return new FileProcessor();
    }

    @Bean
    SessionFactory<LsEntry> sessionFactory() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost("52.171.56.213");
        factory.setPort(22);
        factory.setUser("purecloud");
        factory.setPassword("spld=Lx*wr=nlR1d&2ra");
        factory.setAllowUnknownKeys(true);
        return factory;
    }

}
