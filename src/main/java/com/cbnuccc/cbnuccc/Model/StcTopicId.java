package com.cbnuccc.cbnuccc.Model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StcTopicId implements Serializable {
    private Long stc;

    private Short topicNumber;
}
