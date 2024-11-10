package generated;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * Auto-generated DTO class
 * Generated at: 2024-11-09T22:21:53.084733700
 */
@Data
@Builder
public class UserDto {
    private Double score;
    private String createdAt;
    private String name;
    private String nickname;
    private Boolean isActive;
    private Long userId;
    private Integer age;
    private String email;
    private Integer point;
}
