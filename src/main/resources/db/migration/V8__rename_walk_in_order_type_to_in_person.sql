UPDATE `order`
SET `OrderType` = 'IN_PERSON'
WHERE `OrderType` = 'WALK_IN';

UPDATE `payment`
SET `PaymentType` = 'IN_PERSON'
WHERE `PaymentType` = 'WALK_IN';

UPDATE `order_log`
SET `ActionType` = REPLACE(`ActionType`, 'WALK_IN', 'IN_PERSON')
WHERE `ActionType` LIKE 'WALK_IN%';
