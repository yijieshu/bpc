/*
 * This file is generated by jOOQ.
 */
package brs.schema.tables.records;


import brs.schema.tables.EscrowDecision;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record6;
import org.jooq.Row6;
import org.jooq.impl.UpdatableRecordImpl;

import javax.annotation.Generated;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.11"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class EscrowDecisionRecord extends UpdatableRecordImpl<EscrowDecisionRecord> implements Record6<Long, Long, Long, Integer, Integer, Boolean> {

    private static final long serialVersionUID = 1642215127;

    /**
     * Setter for <code>DB.escrow_decision.db_id</code>.
     */
    public void setDbId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>DB.escrow_decision.db_id</code>.
     */
    public Long getDbId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>DB.escrow_decision.escrow_id</code>.
     */
    public void setEscrowId(Long value) {
        set(1, value);
    }

    /**
     * Getter for <code>DB.escrow_decision.escrow_id</code>.
     */
    public Long getEscrowId() {
        return (Long) get(1);
    }

    /**
     * Setter for <code>DB.escrow_decision.account_id</code>.
     */
    public void setAccountId(Long value) {
        set(2, value);
    }

    /**
     * Getter for <code>DB.escrow_decision.account_id</code>.
     */
    public Long getAccountId() {
        return (Long) get(2);
    }

    /**
     * Setter for <code>DB.escrow_decision.decision</code>.
     */
    public void setDecision(Integer value) {
        set(3, value);
    }

    /**
     * Getter for <code>DB.escrow_decision.decision</code>.
     */
    public Integer getDecision() {
        return (Integer) get(3);
    }

    /**
     * Setter for <code>DB.escrow_decision.height</code>.
     */
    public void setHeight(Integer value) {
        set(4, value);
    }

    /**
     * Getter for <code>DB.escrow_decision.height</code>.
     */
    public Integer getHeight() {
        return (Integer) get(4);
    }

    /**
     * Setter for <code>DB.escrow_decision.latest</code>.
     */
    public void setLatest(Boolean value) {
        set(5, value);
    }

    /**
     * Getter for <code>DB.escrow_decision.latest</code>.
     */
    public Boolean getLatest() {
        return (Boolean) get(5);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record6 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row6<Long, Long, Long, Integer, Integer, Boolean> fieldsRow() {
        return (Row6) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row6<Long, Long, Long, Integer, Integer, Boolean> valuesRow() {
        return (Row6) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field1() {
        return EscrowDecision.ESCROW_DECISION.DB_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field2() {
        return EscrowDecision.ESCROW_DECISION.ESCROW_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field3() {
        return EscrowDecision.ESCROW_DECISION.ACCOUNT_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Integer> field4() {
        return EscrowDecision.ESCROW_DECISION.DECISION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Integer> field5() {
        return EscrowDecision.ESCROW_DECISION.HEIGHT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Boolean> field6() {
        return EscrowDecision.ESCROW_DECISION.LATEST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component1() {
        return getDbId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component2() {
        return getEscrowId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component3() {
        return getAccountId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer component4() {
        return getDecision();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer component5() {
        return getHeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean component6() {
        return getLatest();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value1() {
        return getDbId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value2() {
        return getEscrowId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value3() {
        return getAccountId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer value4() {
        return getDecision();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer value5() {
        return getHeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean value6() {
        return getLatest();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EscrowDecisionRecord value1(Long value) {
        setDbId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EscrowDecisionRecord value2(Long value) {
        setEscrowId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EscrowDecisionRecord value3(Long value) {
        setAccountId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EscrowDecisionRecord value4(Integer value) {
        setDecision(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EscrowDecisionRecord value5(Integer value) {
        setHeight(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EscrowDecisionRecord value6(Boolean value) {
        setLatest(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EscrowDecisionRecord values(Long value1, Long value2, Long value3, Integer value4, Integer value5, Boolean value6) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached EscrowDecisionRecord
     */
    public EscrowDecisionRecord() {
        super(EscrowDecision.ESCROW_DECISION);
    }

    /**
     * Create a detached, initialised EscrowDecisionRecord
     */
    public EscrowDecisionRecord(Long dbId, Long escrowId, Long accountId, Integer decision, Integer height, Boolean latest) {
        super(EscrowDecision.ESCROW_DECISION);

        set(0, dbId);
        set(1, escrowId);
        set(2, accountId);
        set(3, decision);
        set(4, height);
        set(5, latest);
    }
}
