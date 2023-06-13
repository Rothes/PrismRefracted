package network.darkhelmet.prism.database.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.database.PrismDataSource;
import network.darkhelmet.prism.database.sql.SqlPrismDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;

public class PrismHikariDataSource extends SqlPrismDataSource {

    private static final File propFile = new File(Prism.getInstance().getDataFolder(),
            "hikari.properties");
    private static final HikariConfig dbConfig;

    static {
        if (propFile.exists()) {
            Prism.log("正在根据 " + propFile.getName() + " 配置 Hikari");
            dbConfig = new HikariConfig(propFile.getPath());
        } else {
            Prism.log("您需要调整以下配置项以安装 Prism.");
            Prism.log("要更改表的前缀, 您可以编辑 config.yml 中的 datasource.properties.prefix 配置项.");
            String jdbcUrl = "jdbc:mysql://localhost:3306/prism?useUnicode=true&characterEncoding=UTF-8&useSSL=false";
            Prism.log("默认 jdbcUrl: " + jdbcUrl);
            Prism.log("默认 Username: username");
            Prism.log("默认 Password: password");
            Prism.log("您可能需要提供支持您数据库所需的 Jar 库(驱动).");
            dbConfig = new HikariConfig();
            dbConfig.setJdbcUrl(jdbcUrl);
            dbConfig.setUsername("username");
            dbConfig.setPassword("password");
            dbConfig.setMinimumIdle(2);
            dbConfig.setMaximumPoolSize(10);
            HikariHelper.createPropertiesFile(propFile, dbConfig, false);
        }
    }

    /**
     * Create a dataSource.
     *
     * @param section Config
     */
    public PrismHikariDataSource(ConfigurationSection section) {
        super(section);
        name = "hikari";
    }

    @Override
    public PrismDataSource createDataSource() {
        try {
            database = new HikariDataSource(dbConfig);
            createSettingsQuery();
            return this;
        } catch (HikariPool.PoolInitializationException e) {
            Prism.warn("Hikari 数据池未成功初始化: " + e.getMessage());
            database = null;
        } catch (IllegalArgumentException e) {
            Prism.warn("Hikari 数据池未成功初始化: " + e);
            database = null;
        }
        return this;

    }

    @Override
    public void setFile() {

    }
}
