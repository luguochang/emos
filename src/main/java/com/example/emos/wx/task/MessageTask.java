package com.example.emos.wx.task;

import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRefEntity;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.MessageService;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


@Component
@Slf4j
public class MessageTask {
    @Autowired
    private ConnectionFactory factory;
    @Autowired
    private MessageService messageService;

    /**
     * 同步发送消息
     * @param topic 主题
     * @param entity 消息对象
     */
    public void send(String topic, MessageEntity entity){
        String id = messageService.insertMessage(entity);
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()){

            channel.queueDeclare(topic,true,false,false,null);
            HashMap header = new HashMap();
            header.put("messageId",id);
            //创建AMQP协议参数对象，添加附加属性
            AMQP.BasicProperties properties = new AMQP.BasicProperties().builder().headers(header).build();
            channel.basicPublish("",topic,properties,entity.getMsg().getBytes(StandardCharsets.UTF_8));
            log.debug("消息发送成功");
        } catch (Exception e) {
            log.error("执行异常",e);
            throw new EmosException("向MQ发送消息失败");
        }
    }

    /**
     * 异步发送消息
     * @param topic
     * @param entity
     */
    @Async
    public void sendAsync(String topic,MessageEntity entity){
        send(topic,entity);
    }

    /**
     * 同步接收消息
     * @param topic
     * @return
     */
    public int recevie(String topic){
        int i = 0;
        try (Connection connection = factory.newConnection();
        Channel channel = connection.createChannel()){
            //从队列中获取消息，不自动确认
            channel.queueDeclare(topic,true,false,false,null);
            //Topic中有多少数据未知，所以使用死循环接收数据，直到收不到消息
            while (true){
                //创建响应接收数据，禁止自动发送ACK应答
                GetResponse response = channel.basicGet(topic, false);
                if (response != null){
                    AMQP.BasicProperties properties = response.getProps();
                    Map<String, Object> headers = properties.getHeaders();
                    String messageId = headers.get("messageId").toString();
                    byte[] body = response.getBody();//获取消息正文
                    String message = new String(body);
                    log.debug("从RabbitMQ接收的消息: "+message);
                    MessageRefEntity entity = new MessageRefEntity();
                    entity.setMessageId(messageId);
                    entity.setReceiverId(Integer.parseInt(topic));
                    entity.setReadFlag(false);
                    entity.setLastFlag(true);
                    messageService.insertRef(entity);
                    //数据保存到mongodb后，才发送ack应答，让topic删除这条消息
                    long deliveryTag = response.getEnvelope().getDeliveryTag();
                    channel.basicAck(deliveryTag,false);
                    i++;
                }else {
                    break;//接收不到消息，则退出死循环
                }
            }

        }catch (Exception e){
            log.error("执行异常",e);
        }
        return i;
    }

    /**
     * 异步接收数据
     * @param topic
     * @return
     */
    @Async
    public int receiveAsync(String topic){
        return recevie(topic);
    }

    /**
     * 同步删除队列
     * @param topic
     */
    public void deleteQueue(String topic){
        try (Connection connection = factory.newConnection();
        Channel channel = connection.createChannel()){
            channel.queueDelete(topic);
            log.error("消息队列成功删除");
        }catch (Exception e){
            log.error("删除队列失败 ",e);
            throw new EmosException("删除队列失败");
        }
    }

    @Async
    public void deleteQueueAsync(String topic){
        deleteQueue(topic);
    }
}
