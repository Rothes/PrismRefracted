package network.darkhelmet.prism.actionlibs;

import network.darkhelmet.prism.Prism;

import java.sql.Connection;
import java.sql.SQLException;

public class InternalAffairs implements Runnable {

    private final Prism plugin;

    public InternalAffairs(Prism plugin) {
        Prism.debug("[内部状况] 正在持续监视记录器.");
        this.plugin = plugin;
    }

    @Override
    public void run() {

        if (plugin.recordingTask != null && plugin.recordingTask.isActive()) {
            Prism.debug("[内部状况] 记录器目前活动中. 一切都很好.");
            return;
        }

        Prism.log("[内部状况] 记录器目前*不在*活动中. 正在检查数据库...");

        // is db connection valid?
        try (Connection conn = Prism.getPrismDataSource().getConnection()) {
            if (conn == null) {
                Prism.log("[内部状况] 连接池返回了 NULL 而不是一个有效的连接.");
            } else if (conn.isClosed()) {
                Prism.log("[内部状况] 连接池返回了一个已关闭的连接.");
            } else if (conn.isValid(5)) {
                Prism.log("[内部状况] 连接池返回了有效的连接!");
                Prism.log("[内部状况] 正在重启记录器计划任务.");
                plugin.actionRecorderTask();
            }
        } catch (final SQLException e) {
            Prism.debug("[内部状况] 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}