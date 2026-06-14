package com.ticketrush.infrastructure.mysql.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class MyBatisMapperXmlTest {

    @Test
    void shouldParseAllMapperXmlFiles() throws Exception {
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);

        parse(configuration, "mapper/TicketEventMapper.xml");
        parse(configuration, "mapper/TicketSkuMapper.xml");
        parse(configuration, "mapper/TicketInventoryMapper.xml");
        parse(configuration, "mapper/TicketOrderMapper.xml");

        assertThat(configuration.hasStatement(statement(TicketEventMapper.class, "findById"))).isTrue();
        assertThat(configuration.hasStatement(statement(TicketSkuMapper.class, "findByEventId"))).isTrue();
        assertThat(configuration.hasStatement(statement(TicketInventoryMapper.class, "reserveByOptimisticLock"))).isTrue();
        assertThat(configuration.hasStatement(statement(TicketOrderMapper.class, "findExpiredPendingOrders"))).isTrue();
        assertThat(configuration.hasStatement(statement(TicketOrderMapper.class, "closeExpiredOrder"))).isTrue();
    }

    private void parse(Configuration configuration, String resource) throws Exception {
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(
                    inputStream,
                    configuration,
                    resource,
                    configuration.getSqlFragments()
            );
            mapperBuilder.parse();
        }
    }

    private String statement(Class<?> mapperType, String methodName) {
        return mapperType.getName() + "." + methodName;
    }
}
