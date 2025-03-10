/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.operator.scalar;

import io.trino.FeaturesConfig;
import io.trino.Session;
import io.trino.metadata.InternalFunctionBundle;
import io.trino.spi.ErrorCodeSupplier;
import io.trino.spi.function.OperatorType;
import io.trino.spi.type.Type;
import org.intellij.lang.annotations.Language;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.airlift.testing.Closeables.closeAllRuntimeException;
import static io.trino.SessionTestUtils.TEST_SESSION;
import static io.trino.metadata.OperatorNameUtil.mangleOperatorName;
import static io.trino.spi.StandardErrorCode.INVALID_FUNCTION_ARGUMENT;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.testng.Assert.fail;

public abstract class AbstractTestFunctions
{
    protected final Session session;
    private final FeaturesConfig config;
    protected FunctionAssertions functionAssertions;

    protected AbstractTestFunctions()
    {
        this(TEST_SESSION);
    }

    protected AbstractTestFunctions(Session session)
    {
        this(session, new FeaturesConfig());
    }

    protected AbstractTestFunctions(FeaturesConfig config)
    {
        this(TEST_SESSION, config);
    }

    protected AbstractTestFunctions(Session session, FeaturesConfig config)
    {
        this.session = requireNonNull(session, "session is null");
        this.config = requireNonNull(config, "config is null");
    }

    @BeforeClass
    public final void initTestFunctions()
    {
        functionAssertions = new FunctionAssertions(session, config);
    }

    @AfterClass(alwaysRun = true)
    public final void destroyTestFunctions()
    {
        closeAllRuntimeException(functionAssertions);
        functionAssertions = null;
    }

    /**
     * @deprecated Use {@link io.trino.sql.query.QueryAssertions#function(String, String...)}
     */
    @Deprecated
    protected void assertFunction(@Language("SQL") String projection, Type expectedType, Object expected)
    {
        functionAssertions.assertFunction(projection, expectedType, expected);
    }

    /**
     * @deprecated Use {@link io.trino.sql.query.QueryAssertions#operator(OperatorType, String...)}
     */
    @Deprecated
    protected void assertOperator(OperatorType operator, String value, Type expectedType, Object expected)
    {
        functionAssertions.assertFunction(format("\"%s\"(%s)", mangleOperatorName(operator), value), expectedType, expected);
    }

    protected void assertAmbiguousFunction(@Language("SQL") String projection, Type expectedType, Set<Object> expected)
    {
        functionAssertions.assertAmbiguousFunction(projection, expectedType, expected);
    }

    protected void assertInvalidFunction(@Language("SQL") String projection, ErrorCodeSupplier errorCode, String message)
    {
        functionAssertions.assertInvalidFunction(projection, errorCode, message);
    }

    protected void assertInvalidFunction(@Language("SQL") String projection, String message)
    {
        functionAssertions.assertInvalidFunction(projection, INVALID_FUNCTION_ARGUMENT, message);
    }

    protected void assertInvalidFunction(@Language("SQL") String projection, ErrorCodeSupplier expectedErrorCode)
    {
        functionAssertions.assertInvalidFunction(projection, expectedErrorCode);
    }

    protected void assertInvalidCast(@Language("SQL") String projection, String message)
    {
        functionAssertions.assertInvalidCast(projection, message);
    }

    protected void assertCachedInstanceHasBoundedRetainedSize(String projection)
    {
        functionAssertions.assertCachedInstanceHasBoundedRetainedSize(projection);
    }

    protected void registerScalar(Class<?> clazz)
    {
        functionAssertions.addFunctions(InternalFunctionBundle.builder()
                .scalars(clazz)
                .build());
    }

    protected void registerParametricScalar(Class<?> clazz)
    {
        functionAssertions.addFunctions(InternalFunctionBundle.builder()
                .scalar(clazz)
                .build());
    }

    // this help function should only be used when the map contains null value
    // otherwise, use ImmutableMap.of()
    public static <K, V> Map<K, V> asMap(List<K> keyList, List<V> valueList)
    {
        if (keyList.size() != valueList.size()) {
            fail("keyList should have same size with valueList");
        }
        Map<K, V> map = new HashMap<>();
        for (int i = 0; i < keyList.size(); i++) {
            if (map.put(keyList.get(i), valueList.get(i)) != null) {
                fail("keyList should have same size with valueList");
            }
        }
        return map;
    }
}
