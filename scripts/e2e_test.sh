#!/bin/bash

BASE_URL="http://localhost:8080"
PASS=0
FAIL=0
TOTAL=0

echo "Waiting for app to be ready..."
until curl -s "$BASE_URL/actuator/health" | grep -q '"status":"UP"'; do sleep 1; done
echo "App is ready."

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

assert() {
  local label="$1"
  local expected="$2"
  local actual="$3"
  TOTAL=$((TOTAL + 1))
  if [ "$actual" = "$expected" ]; then
    echo -e "  ${GREEN}✔${NC} $label"
    PASS=$((PASS + 1))
  else
    echo -e "  ${RED}✘${NC} $label"
    echo -e "    ${RED}expected: $expected${NC}"
    echo -e "    ${RED}actual:   $actual${NC}"
    FAIL=$((FAIL + 1))
  fi
}

section() {
  echo ""
  echo -e "${CYAN}${BOLD}━━━ $1 ━━━${NC}"
}

# ─────────────────────────────────────────────
section "AUTH — SETUP"
# ─────────────────────────────────────────────

# Setup with missing fields → 400
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/auth/setup" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Admin"}')
assert "setup missing fields → 400" "400" "$R"

# Setup success → 201
R=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/apis/v1/auth/setup" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Alice","lastName":"Admin","email":"alice@company.com","password":"Admin1234!","department":"Management"}')
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "setup success → 201" "201" "$STATUS"
MANAGER_ID=$(echo "$BODY" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

# Setup again → 409
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/auth/setup" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Bob","lastName":"Smith","email":"bob@company.com","password":"Admin1234!","department":"IT"}')
assert "setup again → 409" "409" "$R"

# ─────────────────────────────────────────────
section "AUTH — LOGIN"
# ─────────────────────────────────────────────

# Login wrong password → 401
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@company.com","password":"wrongpassword"}')
assert "login wrong password → 401" "401" "$R"

# Login wrong email → 401
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"nobody@company.com","password":"Admin1234!"}')
assert "login wrong email → 401" "401" "$R"

# Login missing fields → 400
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@company.com"}')
assert "login missing password → 400" "400" "$R"

# Login success as manager → 200 + token
R=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/apis/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@company.com","password":"Admin1234!"}')
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "login manager success → 200" "200" "$STATUS"
MANAGER_TOKEN=$(echo "$BODY" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

# ─────────────────────────────────────────────
section "CURRENCIES"
# ─────────────────────────────────────────────

# No token → 401
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/currencies")
assert "list currencies no token → 401" "401" "$R"

# Manager can list → 200
R=$(curl -s -w "\n%{http_code}" "$BASE_URL/apis/v1/currencies" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "list currencies manager → 200" "200" "$STATUS"
CURRENCY_COUNT=$(echo "$BODY" | grep -o '"code"' | wc -l | tr -d ' ')
assert "list currencies returns seeded data" "1" "$([ "$CURRENCY_COUNT" -gt 0 ] && echo 1 || echo 0)"
USD_CODE=$(echo "$BODY" | grep -o '"code":"USD"' | head -1 | cut -d'"' -f4)
assert "list currencies contains USD" "USD" "$USD_CODE"

# ─────────────────────────────────────────────
section "EMPLOYEES — CREATE"
# ─────────────────────────────────────────────

# No token → 401
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/employees" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Bob","lastName":"Smith","email":"bob@company.com","password":"Pass1234!","role":"EMPLOYEE","department":"IT","position":"Dev","salary":3000,"currencyCode":"USD","hireDate":"2024-01-15"}')
assert "create employee no token → 401" "401" "$R"

# Missing required fields → 400
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/employees" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Bob"}')
assert "create employee missing fields → 400" "400" "$R"

# Invalid currency → 400
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/employees" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Bob","lastName":"Smith","email":"bob@company.com","password":"Pass1234!","role":"EMPLOYEE","department":"IT","position":"Dev","salary":3000,"currencyCode":"XXX","hireDate":"2024-01-15"}')
assert "create employee invalid currency → 400" "400" "$R"

# Create employee (EMPLOYEE role) → 201
R=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/apis/v1/employees" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Bob","lastName":"Smith","email":"bob@company.com","password":"Pass1234!","role":"EMPLOYEE","department":"Engineering","position":"Developer","salary":3000,"currencyCode":"USD","hireDate":"2023-03-01"}')
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "create employee → 201" "201" "$STATUS"
EMPLOYEE_ID=$(echo "$BODY" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
EMPLOYEE_TOKEN=""

# Create HR employee → 201
R=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/apis/v1/employees" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Carol","lastName":"HR","email":"carol@company.com","password":"Pass1234!","role":"HR","department":"HR","position":"HR Manager","salary":4000,"currencyCode":"USD","hireDate":"2023-01-10"}')
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "create HR employee → 201" "201" "$STATUS"
HR_ID=$(echo "$BODY" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

# Duplicate email → 409
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/employees" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Dup","lastName":"User","email":"bob@company.com","password":"Pass1234!","role":"EMPLOYEE","department":"IT","position":"Dev","salary":2000,"currencyCode":"USD","hireDate":"2024-01-15"}')
assert "create employee duplicate email → 409" "409" "$R"

# Login as employee
R=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/apis/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"bob@company.com","password":"Pass1234!"}')
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "login as employee → 200" "200" "$STATUS"
EMPLOYEE_TOKEN=$(echo "$BODY" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

# Login as HR
R=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/apis/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"carol@company.com","password":"Pass1234!"}')
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "login as HR → 200" "200" "$STATUS"
HR_TOKEN=$(echo "$BODY" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

# Employee cannot create employee → 403
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/employees" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"X","lastName":"Y","email":"x@company.com","password":"Pass1234!","role":"EMPLOYEE","department":"IT","position":"Dev","salary":1000,"currencyCode":"USD","hireDate":"2024-01-15"}')
assert "create employee as EMPLOYEE role → 403" "403" "$R"

# HR can create employee → 201
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/employees" \
  -H "Authorization: Bearer $HR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Dave","lastName":"HR","email":"dave@company.com","password":"Pass1234!","role":"EMPLOYEE","department":"Sales","position":"Sales Rep","salary":2500,"currencyCode":"USD","hireDate":"2024-02-01"}')
assert "HR can create employee → 201" "201" "$R"

# ─────────────────────────────────────────────
section "EMPLOYEES — PROFILE (GET /me)"
# ─────────────────────────────────────────────

# No token → 401
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/employees/me")
assert "get profile no token → 401" "401" "$R"

# Employee gets own profile → 200
R=$(curl -s -w "\n%{http_code}" "$BASE_URL/apis/v1/employees/me" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "get own profile → 200" "200" "$STATUS"
PROFILE_EMAIL=$(echo "$BODY" | grep -o '"email":"bob@company.com"' | cut -d'"' -f4)
assert "profile has correct email" "bob@company.com" "$PROFILE_EMAIL"

# ─────────────────────────────────────────────
section "EMPLOYEES — GET BY ID"
# ─────────────────────────────────────────────

# No token → 401
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/employees/$EMPLOYEE_ID")
assert "get employee by id no token → 401" "401" "$R"

# Employee cannot access → 403
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/employees/$EMPLOYEE_ID" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN")
assert "get employee by id as EMPLOYEE → 403" "403" "$R"

# Not found → 404
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/employees/00000000-nonexistent" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
assert "get employee by id not found → 404" "404" "$R"

# Manager gets employee → 200
R=$(curl -s -w "\n%{http_code}" "$BASE_URL/apis/v1/employees/$EMPLOYEE_ID" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "get employee by id manager → 200" "200" "$STATUS"
GOT_ID=$(echo "$BODY" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
assert "get employee by id returns correct id" "$EMPLOYEE_ID" "$GOT_ID"

# ─────────────────────────────────────────────
section "EMPLOYEES — LIST"
# ─────────────────────────────────────────────

# No token → 401
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/employees")
assert "list employees no token → 401" "401" "$R"

# Employee cannot list → 403
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/employees" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN")
assert "list employees as EMPLOYEE → 403" "403" "$R"

# Manager lists all → 200
R=$(curl -s -w "\n%{http_code}" "$BASE_URL/apis/v1/employees" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "list employees manager → 200" "200" "$STATUS"
HAS_ITEMS=$(echo "$BODY" | grep -o '"items":\[' | head -1)
assert "list employees has items array" '"items":[' "$HAS_ITEMS"

# HR lists all → 200
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/employees" \
  -H "Authorization: Bearer $HR_TOKEN")
assert "list employees HR → 200" "200" "$R"

# Filter by role
R=$(curl -s -w "\n%{http_code}" "$BASE_URL/apis/v1/employees?role=MANAGER" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "list employees filter by role → 200" "200" "$STATUS"

# Search by name
R=$(curl -s -w "\n%{http_code}" "$BASE_URL/apis/v1/employees?search=Bob" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "list employees search → 200" "200" "$STATUS"
SEARCH_HIT=$(echo "$BODY" | grep -o '"firstName":"Bob"' | head -1)
assert "search result contains Bob" '"firstName":"Bob"' "$SEARCH_HIT"

# Pagination
R=$(curl -s -w "\n%{http_code}" "$BASE_URL/apis/v1/employees?size=1" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "list employees pagination size=1 → 200" "200" "$STATUS"
HAS_MORE=$(echo "$BODY" | grep -o '"hasMore":true')
assert "list employees hasMore=true with size=1" '"hasMore":true' "$HAS_MORE"

# ─────────────────────────────────────────────
section "EMPLOYEES — UPDATE"
# ─────────────────────────────────────────────

# No token → 401
R=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/apis/v1/employees/$EMPLOYEE_ID" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Updated"}')
assert "update employee no token → 401" "401" "$R"

# Employee role cannot update → 403
R=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/apis/v1/employees/$EMPLOYEE_ID" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Updated"}')
assert "update employee as EMPLOYEE → 403" "403" "$R"

# Not found → 404
R=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/apis/v1/employees/nonexistent-id" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Updated"}')
assert "update employee not found → 404" "404" "$R"

# HR cannot update another HR → 403
R=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/apis/v1/employees/$HR_ID" \
  -H "Authorization: Bearer $HR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Updated"}')
assert "HR update another HR → 403" "403" "$R"

# Manager updates employee → 200
R=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/apis/v1/employees/$EMPLOYEE_ID" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Bobby","department":"Product"}')
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "update employee manager → 200" "200" "$STATUS"
UPDATED_NAME=$(echo "$BODY" | grep -o '"firstName":"Bobby"' | cut -d'"' -f4)
assert "update reflects new first name" "Bobby" "$UPDATED_NAME"

# HR updates employee → 200
R=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/apis/v1/employees/$EMPLOYEE_ID" \
  -H "Authorization: Bearer $HR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"position":"Senior Developer"}')
assert "HR update employee → 200" "200" "$R"

# ─────────────────────────────────────────────
section "EMPLOYEES — CURRENCIES (via HR access)"
# ─────────────────────────────────────────────

# HR lists currencies → 200
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/currencies" \
  -H "Authorization: Bearer $HR_TOKEN")
assert "HR list currencies → 200" "200" "$R"

# Employee lists currencies → 403
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/currencies" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN")
assert "employee list currencies → 403" "403" "$R"

# ─────────────────────────────────────────────
section "LEAVE REQUESTS — SUBMIT"
# ─────────────────────────────────────────────

TODAY=$(date +%Y-%m-%d)
TOMORROW=$(date -d "+1 day" +%Y-%m-%d)
IN_5_DAYS=$(date -d "+5 days" +%Y-%m-%d)
IN_3_DAYS=$(date -d "+3 days" +%Y-%m-%d)
IN_10_DAYS=$(date -d "+10 days" +%Y-%m-%d)
IN_15_DAYS=$(date -d "+15 days" +%Y-%m-%d)
YESTERDAY=$(date -d "-1 day" +%Y-%m-%d)

# No token → 401
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/leave-requests" \
  -H "Content-Type: application/json" \
  -d "{\"startDate\":\"$TOMORROW\",\"endDate\":\"$IN_5_DAYS\",\"type\":\"ANNUAL\"}")
assert "submit leave no token → 401" "401" "$R"

# Missing startDate → 400
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"endDate\":\"$IN_5_DAYS\",\"type\":\"ANNUAL\"}")
assert "submit leave missing startDate → 400" "400" "$R"

# Missing endDate → 400
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"startDate\":\"$TOMORROW\",\"type\":\"ANNUAL\"}")
assert "submit leave missing endDate → 400" "400" "$R"

# Missing type → 400
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"startDate\":\"$TOMORROW\",\"endDate\":\"$IN_5_DAYS\"}")
assert "submit leave missing type → 400" "400" "$R"

# Invalid type → 400
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"startDate\":\"$TOMORROW\",\"endDate\":\"$IN_5_DAYS\",\"type\":\"HOLIDAY\"}")
assert "submit leave invalid type → 400" "400" "$R"

# startDate in the past → 400
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"startDate\":\"$YESTERDAY\",\"endDate\":\"$IN_5_DAYS\",\"type\":\"ANNUAL\"}")
assert "submit leave startDate in past → 400" "400" "$R"

# endDate before startDate → 400 with field error
R=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"startDate\":\"$IN_5_DAYS\",\"endDate\":\"$TOMORROW\",\"type\":\"ANNUAL\"}")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "submit leave endDate before startDate → 400" "400" "$STATUS"
FIELD=$(echo "$BODY" | grep -o '"field":"endDate"' | cut -d'"' -f4)
assert "endDate before startDate field error = endDate" "endDate" "$FIELD"

# reason over 500 chars → 400
LONG_REASON=$(python3 -c "print('a'*501)")
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"startDate\":\"$TOMORROW\",\"endDate\":\"$IN_5_DAYS\",\"type\":\"ANNUAL\",\"reason\":\"$LONG_REASON\"}")
assert "submit leave reason too long → 400" "400" "$R"

# Employee submits ANNUAL leave → 201
R=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"startDate\":\"$TOMORROW\",\"endDate\":\"$IN_5_DAYS\",\"type\":\"ANNUAL\",\"reason\":\"Vacation\"}")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "employee submit ANNUAL leave → 201" "201" "$STATUS"
LEAVE_ID=$(echo "$BODY" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
LEAVE_STATUS=$(echo "$BODY" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
assert "leave status defaults to PENDING" "PENDING" "$LEAVE_STATUS"
LEAVE_TOTAL=$(echo "$BODY" | grep -o '"totalDays":[0-9]*' | cut -d':' -f2)
assert "leave totalDays = 5" "5" "$LEAVE_TOTAL"
LEAVE_MSG=$(echo "$BODY" | grep -o '"message":"Leave request submitted successfully"' | cut -d'"' -f4)
assert "leave message correct" "Leave request submitted successfully" "$LEAVE_MSG"

# Manager can submit leave → 201
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"startDate\":\"$IN_10_DAYS\",\"endDate\":\"$IN_15_DAYS\",\"type\":\"ANNUAL\"}")
assert "manager submit leave → 201" "201" "$R"

# HR can submit leave → 201
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $HR_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"startDate\":\"$IN_10_DAYS\",\"endDate\":\"$IN_15_DAYS\",\"type\":\"ANNUAL\"}")
assert "HR submit leave → 201" "201" "$R"

# Overlapping leave → 409
R=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"startDate\":\"$IN_3_DAYS\",\"endDate\":\"$IN_10_DAYS\",\"type\":\"ANNUAL\"}")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "overlapping leave → 409" "409" "$STATUS"
OVERLAP_FIELD=$(echo "$BODY" | grep -o '"field":"startDate"' | cut -d'"' -f4)
assert "overlap field error = startDate" "startDate" "$OVERLAP_FIELD"
OVERLAP_MSG=$(echo "$BODY" | grep -o '"message":"You already have a leave request overlapping these dates"' | cut -d'"' -f4)
assert "overlap message correct" "You already have a leave request overlapping these dates" "$OVERLAP_MSG"

# Insufficient annual leave balance → 422
# First update employee to have 2 annual leave days
curl -s -o /dev/null -X PUT "$BASE_URL/apis/v1/employees/$EMPLOYEE_ID" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"annualLeaveDays":2}'

# Create a new employee with only 2 days to test fresh
R=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/apis/v1/employees" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Low","lastName":"Balance","email":"low@company.com","password":"Pass1234!","role":"EMPLOYEE","department":"IT","position":"Dev","salary":2000,"currencyCode":"USD","hireDate":"2023-03-01","annualLeaveDays":2}')
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
LOW_ID=$(echo "$BODY" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

R=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/apis/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"low@company.com","password":"Pass1234!"}')
LOW_TOKEN=$(echo "$R" | head -1 | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

R=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $LOW_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"startDate\":\"$TOMORROW\",\"endDate\":\"$IN_5_DAYS\",\"type\":\"ANNUAL\"}")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "insufficient balance → 422" "422" "$STATUS"
BAL_FIELD=$(echo "$BODY" | grep -o '"field":"totalDays"' | cut -d'"' -f4)
assert "insufficient balance field = totalDays" "totalDays" "$BAL_FIELD"
BAL_MSG=$(echo "$BODY" | grep -o '"message":"Insufficient leave balance"' | cut -d'"' -f4)
assert "insufficient balance message correct" "Insufficient leave balance" "$BAL_MSG"

# SICK leave does not check balance → 201
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $LOW_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"startDate\":\"$TOMORROW\",\"endDate\":\"$IN_5_DAYS\",\"type\":\"SICK\"}")
assert "SICK leave ignores balance → 201" "201" "$R"

# UNPAID leave does not check balance → 201
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $LOW_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"startDate\":\"$IN_10_DAYS\",\"endDate\":\"$IN_15_DAYS\",\"type\":\"UNPAID\"}")
assert "UNPAID leave ignores balance → 201" "201" "$R"

# ─────────────────────────────────────────────
section "LEAVE REQUESTS — LIST"
# ─────────────────────────────────────────────

# No token → 401
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/leave-requests")
assert "list leave requests no token → 401" "401" "$R"

# Employee can list → 200 (open to all authenticated)
R=$(curl -s -w "\n%{http_code}" "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "list leave requests as EMPLOYEE → 200" "200" "$STATUS"
HAS_ITEMS=$(echo "$BODY" | grep -o '"items":\[' | head -1)
assert "list leave requests has items array" '"items":[' "$HAS_ITEMS"

# Manager can list → 200
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
assert "list leave requests as MANAGER → 200" "200" "$R"

# HR can list → 200
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $HR_TOKEN")
assert "list leave requests as HR → 200" "200" "$R"

# Returns submitted leaves
R=$(curl -s -w "\n%{http_code}" "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "list leave requests returns data → 200" "200" "$STATUS"
LIST_COUNT=$(echo "$BODY" | grep -o '"id"' | wc -l | tr -d ' ')
assert "list leave requests returns items" "1" "$([ "$LIST_COUNT" -gt 0 ] && echo 1 || echo 0)"

# Filter by status=PENDING
R=$(curl -s -w "\n%{http_code}" "$BASE_URL/apis/v1/leave-requests?status=PENDING" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "filter by status=PENDING → 200" "200" "$STATUS"
STATUS_HIT=$(echo "$BODY" | grep -o '"status":"PENDING"' | head -1 | cut -d'"' -f4)
assert "filtered results contain PENDING status" "PENDING" "$STATUS_HIT"

# Filter by type=ANNUAL
R=$(curl -s -w "\n%{http_code}" "$BASE_URL/apis/v1/leave-requests?type=ANNUAL" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "filter by type=ANNUAL → 200" "200" "$STATUS"
TYPE_HIT=$(echo "$BODY" | grep -o '"type":"ANNUAL"' | head -1 | cut -d'"' -f4)
assert "filtered results contain ANNUAL type" "ANNUAL" "$TYPE_HIT"

# Filter by type=SICK
R=$(curl -s -w "\n%{http_code}" "$BASE_URL/apis/v1/leave-requests?type=SICK" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "filter by type=SICK → 200" "200" "$STATUS"
SICK_HIT=$(echo "$BODY" | grep -o '"type":"SICK"' | head -1 | cut -d'"' -f4)
assert "filtered results contain SICK type" "SICK" "$SICK_HIT"

# Filter by employeeId
R=$(curl -s -w "\n%{http_code}" "$BASE_URL/apis/v1/leave-requests?employeeId=$EMPLOYEE_ID" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "filter by employeeId → 200" "200" "$STATUS"
EMP_HIT=$(echo "$BODY" | grep -o "\"id\":\"$EMPLOYEE_ID\"" | head -1 | cut -d'"' -f4)
assert "filter by employeeId returns correct employee" "$EMPLOYEE_ID" "$EMP_HIT"

# Pagination size=1 → hasMore true
R=$(curl -s -w "\n%{http_code}" "$BASE_URL/apis/v1/leave-requests?size=1" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "list leave requests size=1 → 200" "200" "$STATUS"
HAS_MORE=$(echo "$BODY" | grep -o '"hasMore":true')
assert "list leave requests hasMore=true with size=1" '"hasMore":true' "$HAS_MORE"
NEXT_CURSOR=$(echo "$BODY" | grep -o '"nextCursor":"[^"]*"' | cut -d'"' -f4)
assert "list leave requests has nextCursor" "1" "$([ -n "$NEXT_CURSOR" ] && echo 1 || echo 0)"

# Next page with cursor returns results
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/leave-requests?size=1&cursor=$NEXT_CURSOR" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
assert "list leave requests with cursor → 200" "200" "$R"

# Invalid status → 400
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/leave-requests?status=INVALID" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
assert "list leave requests invalid status → 400" "400" "$R"

# Invalid type → 400
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/leave-requests?type=INVALID" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
assert "list leave requests invalid type → 400" "400" "$R"

# Size exceeds max → 400
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/leave-requests?size=200" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
assert "list leave requests size too large → 400" "400" "$R"

# Invalid cursor format → 400
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/leave-requests?cursor=not-a-uuid" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
assert "list leave requests invalid cursor → 400" "400" "$R"

# Invalid employeeId format → 400
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/leave-requests?employeeId=not-a-uuid" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
assert "list leave requests invalid employeeId → 400" "400" "$R"

# Default employeeStatus=ACTIVE → returns leaves
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/leave-requests" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
assert "list leaves default (ACTIVE employees) → 200" "200" "$R"

# Explicit employeeStatus=ACTIVE → 200
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/leave-requests?employeeStatus=ACTIVE" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
assert "list leaves employeeStatus=ACTIVE → 200" "200" "$R"

# employeeStatus=INACTIVE → 200 (no items yet since no deactivated employees)
R=$(curl -s -w "\n%{http_code}" "$BASE_URL/apis/v1/leave-requests?employeeStatus=INACTIVE" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "list leaves employeeStatus=INACTIVE → 200" "200" "$STATUS"
INACTIVE_COUNT=$(echo "$BODY" | grep -o '"id"' | wc -l | tr -d ' ')
assert "no inactive employee leaves yet" "0" "$INACTIVE_COUNT"

# Invalid employeeStatus → 400
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/apis/v1/leave-requests?employeeStatus=INVALID" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
assert "list leaves invalid employeeStatus → 400" "400" "$R"

# ─────────────────────────────────────────────
section "EMPLOYEES — DEACTIVATE"
# ─────────────────────────────────────────────

# No token → 401
R=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/apis/v1/employees/$EMPLOYEE_ID")
assert "deactivate no token → 401" "401" "$R"

# Employee cannot deactivate → 403
R=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/apis/v1/employees/$EMPLOYEE_ID" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN")
assert "deactivate as EMPLOYEE → 403" "403" "$R"

# HR cannot deactivate manager → 403
R=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/apis/v1/employees/$MANAGER_ID" \
  -H "Authorization: Bearer $HR_TOKEN")
assert "HR deactivate manager → 403" "403" "$R"

# Not found → 404
R=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/apis/v1/employees/nonexistent-id" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
assert "deactivate not found → 404" "404" "$R"

# Deactivate employee → 200
R=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/apis/v1/employees/$EMPLOYEE_ID" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
STATUS=$(echo "$R" | tail -1)
BODY=$(echo "$R" | head -1)
assert "deactivate employee → 200" "200" "$STATUS"
DEACT_STATUS=$(echo "$BODY" | grep -o '"status":"INACTIVE"' | cut -d'"' -f4)
assert "deactivated employee status = INACTIVE" "INACTIVE" "$DEACT_STATUS"

# Deactivate again → 409
R=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/apis/v1/employees/$EMPLOYEE_ID" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
assert "deactivate already inactive → 409" "409" "$R"

# Inactive employee cannot login → 401
R=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/apis/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"bob@company.com","password":"Pass1234!"}')
assert "inactive employee cannot login → 401" "401" "$R"

# ─────────────────────────────────────────────
echo ""
echo -e "${CYAN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BOLD}  E2E TEST REPORT${NC}"
echo -e "${CYAN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "  Total:   ${BOLD}$TOTAL${NC}"
echo -e "  ${GREEN}Passed:  $PASS${NC}"
if [ "$FAIL" -gt 0 ]; then
  echo -e "  ${RED}Failed:  $FAIL${NC}"
else
  echo -e "  Failed:  $FAIL"
fi
echo -e "${CYAN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
if [ "$FAIL" -eq 0 ]; then
  echo -e "  ${GREEN}${BOLD}ALL TESTS PASSED ✔${NC}"
else
  echo -e "  ${RED}${BOLD}$FAIL TEST(S) FAILED ✘${NC}"
fi
echo -e "${CYAN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
