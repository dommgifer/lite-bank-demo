#!/bin/bash

# API Gateway Test Runner Script
# This script provides different test execution options

set -e

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  API Gateway Test Runner${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Function to check Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        echo -e "${RED}Error: Docker is not running. Please start Docker Desktop.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Docker is running${NC}"
}

# Function to run Maven in Docker
run_maven() {
    docker run --rm \
        -v "${SCRIPT_DIR}:/app" \
        --network host \
        -w /app \
        maven:3.9-eclipse-temurin-21 \
        mvn "$@"
}

# Function to run unit tests only
run_unit_tests() {
    echo -e "${YELLOW}Running Unit Tests (excluding E2E)...${NC}"
    run_maven clean test \
        -Dtest='!com.litebank.gateway.e2e.*' \
        -DfailIfNoTests=false
}

# Function to run E2E tests
run_e2e_tests() {
    echo -e "${YELLOW}Running E2E Tests...${NC}"
    echo -e "${YELLOW}Note: Tests will connect to running services via API Gateway${NC}"
    echo -e "${YELLOW}      Make sure API Gateway is running on port 9000${NC}"
    echo -e "${YELLOW}      Estimated time: 5-10 minutes${NC}"
    echo ""

    check_docker

    run_maven test \
        -Dtest='com.litebank.gateway.e2e.**' \
        -Dsurefire.failIfNoSpecifiedTests=false
}

# Function to run specific E2E test
run_specific_e2e_test() {
    local test_name=$1
    echo -e "${YELLOW}Running specific E2E test: ${test_name}${NC}"

    check_docker

    run_maven test \
        -Dtest="${test_name}" \
        -DfailIfNoTests=false
}

# Function to run all tests
run_all_tests() {
    echo -e "${YELLOW}Running ALL Tests (Unit + E2E)...${NC}"
    echo -e "${YELLOW}Warning: This may take 10-15 minutes${NC}"
    echo ""

    check_docker

    run_maven clean test
}

# Function to compile tests only
compile_tests() {
    echo -e "${YELLOW}Compiling tests...${NC}"
    run_maven test-compile
}

# Main menu
show_menu() {
    echo ""
    echo "Choose test execution mode:"
    echo "  1) Compile tests only (fast, ~30 seconds)"
    echo "  2) Run unit tests only (fast, ~2 minutes)"
    echo "  3) Run E2E tests only (slow, ~10-15 minutes, requires Docker)"
    echo "  4) Run specific E2E test"
    echo "  5) Run ALL tests (unit + E2E, ~15-20 minutes)"
    echo "  6) Exit"
    echo ""
}

# Parse command line arguments
if [ $# -eq 0 ]; then
    # Interactive mode
    while true; do
        show_menu
        read -p "Enter choice [1-6]: " choice

        case $choice in
            1)
                compile_tests
                ;;
            2)
                run_unit_tests
                ;;
            3)
                run_e2e_tests
                ;;
            4)
                echo ""
                echo "Available E2E tests:"
                echo "  - AuthenticationE2ETest"
                echo "  - AccountManagementE2ETest"
                echo "  - DepositWithdrawalE2ETest"
                echo "  - TransferE2ETest"
                echo "  - ExchangeE2ETest"
                echo "  - TransactionHistoryE2ETest"
                echo "  - FullUserJourneyE2ETest"
                echo ""
                read -p "Enter test class name: " test_name
                run_specific_e2e_test "$test_name"
                ;;
            5)
                run_all_tests
                ;;
            6)
                echo -e "${GREEN}Goodbye!${NC}"
                exit 0
                ;;
            *)
                echo -e "${RED}Invalid choice. Please enter 1-6.${NC}"
                ;;
        esac

        echo ""
        read -p "Press Enter to continue..."
    done
else
    # Command line mode
    case $1 in
        compile)
            compile_tests
            ;;
        unit)
            run_unit_tests
            ;;
        e2e)
            run_e2e_tests
            ;;
        all)
            run_all_tests
            ;;
        *)
            echo "Usage: $0 {compile|unit|e2e|all}"
            echo "  compile - Compile tests only"
            echo "  unit    - Run unit tests"
            echo "  e2e     - Run E2E tests"
            echo "  all     - Run all tests"
            exit 1
            ;;
    esac
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Test execution completed!${NC}"
echo -e "${GREEN}========================================${NC}"
