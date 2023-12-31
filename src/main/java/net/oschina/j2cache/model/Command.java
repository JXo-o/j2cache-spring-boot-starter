package net.oschina.j2cache.model;

import java.security.SecureRandom;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * ClassName: Command
 * Package: net.oschina.j2cache.model
 * Description: 命令消息封装
 *
 * @author JX
 * @version 1.0
 * @date 2023/10/23 0:55
 */
public class Command {

    private final static Logger log = LoggerFactory.getLogger(Command.class);

    public final static byte OPT_JOIN 	   = 0x01;	//加入集群
    public final static byte OPT_EVICT_KEY = 0x02; 	//删除缓存
    public final static byte OPT_CLEAR_KEY = 0x03; 	//清除缓存
    public final static byte OPT_QUIT 	   = 0x04;	//退出集群

    private int src;
    private int operator;
    private String region;
    private String[] keys;

    public static int genRandomSrc() {
        long ct = System.currentTimeMillis();
        SecureRandom rndSeed = new SecureRandom();
        return (int) (rndSeed.nextInt(10000) * 1000 + ct % 1000);
    }

    public Command(){}//just for json deserialize , dont remove it.

    public Command(byte o, String r, String...keys){
        this.operator = o;
        this.region = r;
        this.keys = keys;
    }

    public static Command join() {
        return new Command(OPT_JOIN, null);
    }

    public static Command quit() {
        return new Command(OPT_QUIT, null);
    }

    public String json() {
        return JSON.toJSONString(this);
    }

    public static Command parse(String json) {
        try {
            return JSON.parseObject(json, Command.class);
        } catch (JSONException e) {
            log.warn("Failed to parse j2cache command: {}", json, e);
        }
        return null;
    }

    public int getOperator() {
        return operator;
    }

    public String getRegion() {
        return region;
    }

    public String[] getKeys() {
        return keys;
    }

    public int getSrc() {
        return src;
    }

    public void setSrc(int src) {
        this.src = src;
    }

    public void setOperator(int operator) {
        this.operator = operator;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setKeys(String[] keys) {
        this.keys = keys;
    }

    @Override
    public String toString(){
        return json();
    }

}

