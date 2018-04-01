package cn.atme8.blockchain.core;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class BlockChain {
    // 用来存储区块
    public static List<Block> lBlockchain = new ArrayList<Block>();

    // 对局域网内的电脑进行扫描，找到最长的链，下载到本地
    static final String sIPPre = "192.168.0.";

//    static final String sDataFileDir = "c:/blockchain"; // 本地存储路径
    static final String sDataFileDir = "/home/hadoop/blockchain"; // 本地存储路径

    public BlockChain() {
    }

    // 创建新块
    public static Block newBlock(int index, String proof, String hash, Timestamp c, String sender, String recipient) {
        Block block = null;
        // 在这里创建一个新块
        block = new Block(index, proof, hash, c, sender, recipient);

        return block;
    }

    // 创世块的创建，创世块是一个块，必须是固定的信息
    // 逻辑上来说，只有在区块链产品的第一个用户第一次启动的时候，才会需要创建创世块
    public static Block createFirstBlock() {
        try {
            Timestamp t = new Timestamp(new Date().getTime());
            return newBlock(0, "海阔天空", "*", t, "*", "*");
        } catch (Exception e) {
            return null;
        }
    }

    // Hash 一个块
    public static String hash(Block block) {
        String sHash = null;

        // 在这里hash一个块
        String s = block.sPreviousHash + block.sProof + block.sRecipient + block.sSender + block.tsCreateTime.toString();
        sHash = MD5(s);

        return sHash;
    }

    public static String MD5(String key) {
        char hexDigits[] = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        try {
            byte[] btInput = key.getBytes();
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(btInput);
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }

    // 验证当前的成语是否符合规则
    // pre 前一个成语
    // cur 当前成语
    public static String validProof(String pre, String cur) {
        // 验证是否是成语
        // http://chengyu.t086.com/chaxun.php?q=%B9%E2%C3%F7%D5%FD%B4%F3&t=ChengYu
        String content = "";
        try {
            String url = "http://chengyu.t086.com/chaxun.php?q=" + URLEncoder.encode(cur, "GBK") + "&t=ChengYu";
            // System.out.println(url);
            content = httpRequest(url, "GBK");
            // System.out.println(content);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (content.indexOf("没有找到与您搜索相关的成语") != -1 || content.indexOf("搜索词太长") != -1) {
            return "你输入的不是成语，请重新输入...";
        }

        // 验证这个成语的头一个字是不是上一个成语的最后一个字
        if (cur.charAt(0) != pre.charAt(pre.length() - 1)) {
            return "非常遗憾！你晚了一步，已经有其他用户回答了这个成语";
        }

        return "";
    }

    public static String loadData() {
        StringBuffer data = new StringBuffer();
        try {
            InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(new File(sDataFileDir + "//data.txt")));
            BufferedReader br = new BufferedReader(reader);
            String line = "";
            while (true) {
                line = br.readLine();
                if (line == null) {
                    break;
                }
                data.append(line).append("##");
            }
        } catch (Exception e) {
        }
        return data.toString();
    }

    // 从网络读取区块链数据到本地文件
    public static void downloadData() {

        // 检查数据文件目录，不存在就创建
        File dirFile = new File(sDataFileDir);
        boolean bFile = dirFile.exists();
        if (!bFile) {
            bFile = dirFile.mkdir();
            // 往新创建的本地文件里面写一个创世块
            try {
                FileOutputStream out = new FileOutputStream(new File(dirFile + "//data.txt"));
                Block firstBlock = BlockChain.createFirstBlock();
                // System.out.println(firstBlock.toInfoString());
                lBlockchain.add(firstBlock);
                out.write((BlockChain.createFirstBlock().toInfoString() + "\r\n").getBytes());
                out.close();
            } catch (Exception e) {
            }
        }

        // 扫描周边的节点，找到最长的链，下载到本地
        int iLastLen = lBlockchain.size(); // 本地区块链长度
        String sLastChain = "";
        for (int i = 6; i <= 9; i += 1) {
            String sThisURL = "http://" + sIPPre + i + ":8080/blockchain/gainBlockChain";
            System.out.println(sThisURL);

            String sChain = httpRequest(sThisURL, "UTF-8");
            if (sChain != "") {
                System.out.println(sChain);
                String[] sBlocks = sChain.split("##");
                if (sBlocks.length > iLastLen) {
                    iLastLen = sBlocks.length;
                    sLastChain = sChain;
                } else if (sBlocks.length == iLastLen && !"".equals(sLastChain)) {
                    String sBlock = sBlocks[sBlocks.length - 1];

                    String[] sLastBlocks = sLastChain.split("##");
                    String sLastBlock = sLastBlocks[sLastBlocks.length - 1];

                    String[] sBlockItems = sBlock.split("#");
                    String[] sLastBlockItems = sLastBlock.split("#");
                    if (Long.parseLong(sBlockItems[3]) < Long.parseLong(sLastBlockItems[3])) {
                        iLastLen = sBlocks.length;
                        sLastChain = sChain;
                    }
                }
            }
        }

        try {
            if (sLastChain != "") {
                String[] sBlocks = sLastChain.split("##");
                lBlockchain.clear();
                for (String sBlock : sBlocks) {
                    String[] sBlockItems = sBlock.split("#");
                    Block block = new Block(Integer.parseInt(sBlockItems[0]), sBlockItems[1], sBlockItems[2], new Timestamp(Long.parseLong(sBlockItems[3])), sBlockItems[4], sBlockItems[5]);
                    lBlockchain.add(block);
                }

                FileOutputStream out = new FileOutputStream(new File(dirFile + "//data.txt"));
                sLastChain = sLastChain.replace("##", "\r\n");
                out.write((sLastChain + "\r\n").getBytes());
                out.close();
            }
        } catch (Exception e) {
        }

        if (lBlockchain.isEmpty()) {
            try {
                InputStreamReader reader = new InputStreamReader(
                        new FileInputStream(new File(sDataFileDir + "//data.txt")));
                BufferedReader br = new BufferedReader(reader);
                String line = "";
                while (true) {
                    line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    String[] sBlockItems = line.split("#");
                    Block block = new Block(Integer.parseInt(sBlockItems[0]), sBlockItems[1], sBlockItems[2], new Timestamp(Long.parseLong(sBlockItems[3])), sBlockItems[4], sBlockItems[5]);
                    lBlockchain.add(block);
                }
            } catch (Exception e) {
            }
        }
    }

    public static String httpRequest(String url, String encoding) {
        String result = "";
        BufferedReader in = null;
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.75 Safari/537.36");

            connection.setRequestProperty("Accept-Charset", encoding);
            connection.setRequestProperty("contentType", encoding);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
            /*
            for (String key : map.keySet()) {
                System.out.println(key + "--->" + map.get(key));
            }
            */
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), encoding));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            // e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static void writeData() {
        try {
            String filePath = sDataFileDir + "//data.txt";
            File file = new File(filePath);
            if (file.isFile() && file.exists()) {
                file.delete();
            }

            FileOutputStream out = new FileOutputStream(new File(filePath));
            for (Block block : lBlockchain) {
                out.write((block.toInfoString() + "\r\n").getBytes());
            }
            out.close();
        } catch (Exception e) {
        }
    }
}
