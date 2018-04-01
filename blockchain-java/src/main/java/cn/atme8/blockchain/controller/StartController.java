package cn.atme8.blockchain.controller;

import cn.atme8.blockchain.core.Block;
import cn.atme8.blockchain.core.BlockChain;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@Controller
@EnableAutoConfiguration
public class StartController {

    @RequestMapping("/")
    String home() {
        return "index";
    }

    @RequestMapping("/chain")
    String chain(HttpServletRequest request, HttpServletResponse response, Map<String, Object> resultMap) {
        String phone = request.getParameter("phone");
        // 加载区块链数据
        BlockChain.downloadData();

        int score = 0;
        StringBuffer proofChain = new StringBuffer();
        for (Block block : BlockChain.lBlockchain) {
            proofChain.append(block.getsProof()).append("->");
            if (block.getsRecipient().equals(phone)) {
                score += Block.iMoneyAward + Block.iMoneyWin;
            }
        }

        request.getSession().setAttribute("phone", phone);
        resultMap.put("phone", phone);
        resultMap.put("proofChain", proofChain);
        resultMap.put("score", score);
        return "chain";
    }

    @RequestMapping("/gainBlockChain")
    @ResponseBody
    String gainBlockChain(HttpServletRequest request, HttpServletResponse response) {
        String data = BlockChain.loadData();
        return data;
    }

    @RequestMapping("/dig")
    @ResponseBody
    Map<String, Object> dig(HttpServletRequest request, HttpServletResponse response) {
        String answer = request.getParameter("answer");

        // 加载区块链数据
        BlockChain.downloadData();

        // 获取区块链中最后一个成语
        String sPre = BlockChain.lBlockchain.get(BlockChain.lBlockchain.size() - 1).getsProof();

        // 用户输入的成语
        String sCur = new String(answer);

        Map<String, Object> resultMap = new HashMap<String, Object>();
        // 判断是不是正确答案，如果是，就要创建新块并添加到区块链中

        // 验证这个成语的头一个字是不是上一个成语的最后一个字
        String message = BlockChain.validProof(sPre, sCur);
        if ("".equals(message)) {

            Block bPre = BlockChain.lBlockchain.get(BlockChain.lBlockchain.size() - 1);

            // 获取前一个块的Hash
            String sHash = BlockChain.hash(bPre);

            // 创建新块
            Block bCur = BlockChain.newBlock(bPre.getiIndex() + 1, sCur, sHash, new Timestamp(System.currentTimeMillis()), bPre.getsRecipient(), (String) request.getSession().getAttribute("phone"));

            // 加入区块链并保存到本地文件
            BlockChain.lBlockchain.add(bCur);
            BlockChain.writeData();

            resultMap.put("message", "恭喜你接龙成功！");
        } else {
            resultMap.put("message", message);
        }

        int score = 0;
        String phone = (String) request.getSession().getAttribute("phone");
        StringBuffer proofChain = new StringBuffer();
        for (Block block : BlockChain.lBlockchain) {
            proofChain.append(block.getsProof()).append("->");
            if (block.getsRecipient().equals(phone)) {
                score += Block.iMoneyAward + Block.iMoneyWin;
            }
        }
        resultMap.put("proofChain", proofChain);
        resultMap.put("score", score);

        return resultMap;
    }

}
