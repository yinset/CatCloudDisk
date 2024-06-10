package icu.niunai;

import icu.niunai.utils.RedisUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableScheduling
public class Pan2Application {

    public static void main(String[] args) {
        SpringApplication.run(Pan2Application.class, args);
    }

    @Scheduled(cron = "1 */3 * * * ?")
    public void keepAlive() {
        RedisUtil.setEx("keepAlive", "1", 5, TimeUnit.MINUTES);
    }
}
