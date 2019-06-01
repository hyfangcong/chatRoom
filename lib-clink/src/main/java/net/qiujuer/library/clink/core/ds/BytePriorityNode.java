package net.qiujuer.library.clink.core.ds;

/**
 * @author: fangcong
 * @date: 2019/6/1
 */

/**
 * 带有优先级的节点，用于构建链表
 */
public class BytePriorityNode<Item> {
    public byte priority;
    public Item item;
    public BytePriorityNode<Item> next;

    public BytePriorityNode(Item item) {
        this.item = item;
    }

    /**
     * 按优先级将节点追加到链表中
     *
     */
    public void appendWithPriority(BytePriorityNode<Item> node){
        if(next == null){
            next = node;
        }else{
            BytePriorityNode<Item> after = this.next;
            if(after.priority < node.priority){
                //中间位置插入
                this.next = node;
                node.next = after;
            }else
                appendWithPriority(node);
        }
    }
}
