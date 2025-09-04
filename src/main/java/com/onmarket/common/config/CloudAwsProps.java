package com.onmarket.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter
@Component
@ConfigurationProperties(prefix = "cloud.aws")
public class CloudAwsProps {
    private RegionProps region = new RegionProps();
    private StackProps stack = new StackProps();
    private Credentials credentials = new Credentials();
    private S3Props s3 = new S3Props();

    @Getter @Setter
    public static class RegionProps { private String static_; } // yml: region.static
    @Getter @Setter
    public static class StackProps { private boolean auto; }
    @Getter @Setter
    public static class Credentials { private String accessKey; private String secretKey; }
    @Getter @Setter
    public static class S3Props { private String bucket; }
}
