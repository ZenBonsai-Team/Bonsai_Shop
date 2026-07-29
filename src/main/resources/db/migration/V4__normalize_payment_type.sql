UPDATE `payment`
SET `PaymentType` = 'FULL_PAYMENT'
WHERE UPPER(`PaymentType`) = 'IN_PERSON';
