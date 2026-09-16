package co.sena.edu.themis.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración para la integración con Olympo gRPC
 */
@Configuration
public class OlympoGrpcConfig {

    /**
     * Configuración de propiedades para el cliente gRPC de Olympo
     */
    @Bean
    @ConfigurationProperties(prefix = "grpc.client.olympo")
    public OlympoClientProperties olympoClientProperties() {
        return new OlympoClientProperties();
    }

    /**
     * Clase de propiedades para configurar la conexión con Olympo
     */
    public static class OlympoClientProperties {
        private String address = "localhost:9090";
        private boolean enableKeepAlive = true;
        private long keepAliveTime = 30000;
        private boolean keepAliveWithoutCalls = true;
        private int maxInboundMessageSize = 4194304;
        private boolean usePlaintext = true;

        // Getters y setters
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public boolean isEnableKeepAlive() { return enableKeepAlive; }
        public void setEnableKeepAlive(boolean enableKeepAlive) { this.enableKeepAlive = enableKeepAlive; }

        public long getKeepAliveTime() { return keepAliveTime; }
        public void setKeepAliveTime(long keepAliveTime) { this.keepAliveTime = keepAliveTime; }

        public boolean isKeepAliveWithoutCalls() { return keepAliveWithoutCalls; }
        public void setKeepAliveWithoutCalls(boolean keepAliveWithoutCalls) { this.keepAliveWithoutCalls = keepAliveWithoutCalls; }

        public int getMaxInboundMessageSize() { return maxInboundMessageSize; }
        public void setMaxInboundMessageSize(int maxInboundMessageSize) { this.maxInboundMessageSize = maxInboundMessageSize; }

        public boolean isUsePlaintext() { return usePlaintext; }
        public void setUsePlaintext(boolean usePlaintext) { this.usePlaintext = usePlaintext; }
    }
}
