package cn.muzisheng.pear.entity;

import cn.muzisheng.pear.model.Profile;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.mapping.Map;

import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {
    // 主键
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Integer ID;
    // 创建时间
    @Column(columnDefinition = "TIMESTAMP")
    private Date gmtCreated;

    // 更新时间
    @Column(columnDefinition = "TIMESTAMP")
    private Date gmtModified;

    // 邮箱
    private String email;
    // 密码
    private String password;
    // 电话
    private String phone;
    // 英文姓
    private String firstName;
    // 英文名
    private String lastName;
    // 用户显示名
    private String displayName;
    // 是否为管理员身份
    @Column(columnDefinition = "BOOLEAN")
    private boolean isSuperUser;

    // 是否为员工
    @Column(columnDefinition = "BOOLEAN")
    private boolean isStaff;

    // 状态是否正常
    @Column(columnDefinition = "BOOLEAN")
    private boolean enabled;

    // 是否激活
    @Column(columnDefinition = "BOOLEAN")
    private boolean activated;

    // 最后一次登陆时间
    private Date lastLogin;
    // 最后一次登陆ip
    private String lastLoginIp;
    // 用户来源渠道
    private String source;
    // 语言环境
    private String locale;
    // 时区
    private String timezone;
    // 用户信息
    private Profile profile;
    // 用户认证令牌
    private String token;
    public User(String username, String password){
        this.password = password;
        this.email = username;
    }
}
